package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProductsServiceStack extends Stack {
    private static final int SERVER_PORT = 8080;

    public ProductsServiceStack(final Construct scope, final String id, final StackProps props,
                                ProductsServiceProps productsServiceProps) {
        super(scope, id, props);

        FargateTaskDefinition fargateTaskDefinition = new FargateTaskDefinition(this, "TaskDefinition",
                FargateTaskDefinitionProps.builder()
                        .family("products-service")
                        .cpu(512)
                        .memoryLimitMiB(1024)
                        .build());

        AwsLogDriver logDriver = new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(new LogGroup(this, "LogGroup", LogGroupProps.builder()
                        .logGroupName("ProductsService")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_MONTH)
                        .build()))
                .streamPrefix("ProductsService")
                .build());

        fargateTaskDefinition.addContainer("ProductsServiceContainer",
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromEcrRepository(productsServiceProps.repository(), "1.0.0"))
                        .containerName("productsService")
                        .logging(logDriver)
                        .portMappings(List.of(PortMapping.builder()
                                        .containerPort(SERVER_PORT)
                                        .protocol(Protocol.TCP)
                                .build()))
                        .environment(Map.of("SERVER_PORT", String.valueOf(SERVER_PORT)))
                        .build());

        ApplicationListener applicationListener = productsServiceProps.applicationLoadBalancer
                .addListener("ProductsServiceAlbListener", ApplicationListenerProps.builder()
                        .port(SERVER_PORT)
                        .protocol(ApplicationProtocol.HTTP)
                        .loadBalancer(productsServiceProps.applicationLoadBalancer())
                        .build());

        FargateService fargateService = new FargateService(this, "ProductsService",
                FargateServiceProps.builder()
                        .serviceName("ProductsService")
                        .cluster(productsServiceProps.cluster())
                        .taskDefinition(fargateTaskDefinition)
                        .desiredCount(2)
                        .assignPublicIp(true) // Just for study
                        .build());
        productsServiceProps.repository().grantPull(Objects.requireNonNull(fargateTaskDefinition.getExecutionRole()));
        fargateService.getConnections().getSecurityGroups().get(0).addIngressRule(Peer.anyIpv4(), Port.tcp(SERVER_PORT));

        applicationListener.addTargets("ProductsServiceAlbTarget",
                AddApplicationTargetsProps.builder()
                        .targetGroupName("productsServiceAlb")
                        .port(SERVER_PORT)
                        .protocol(ApplicationProtocol.HTTP)
                        .targets(List.of(fargateService))
                        .deregistrationDelay(Duration.seconds(30))
                        .healthCheck(HealthCheck.builder()
                                .enabled(true)
                                .interval(Duration.seconds(30))
                                .timeout(Duration.seconds(10))
                                .path("/actuator/health")
                                .port(String.valueOf(SERVER_PORT))
                                .build())
                        .build());

        NetworkListener networkListener = productsServiceProps.networkLoadBalancer
                .addListener("ProductsServiceNlbListener", BaseNetworkListenerProps.builder()
                        .port(SERVER_PORT)
                        .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP)
                        .build());

        networkListener.addTargets("ProductsServiceNlbTarget", AddNetworkTargetsProps.builder()
                .port(SERVER_PORT)
                .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP)
                .targetGroupName("productsServiceNlb")
                .targets(List.of(fargateService.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                        .containerName("productsService")
                        .containerPort(SERVER_PORT)
                        .protocol(Protocol.TCP)
                        .build())))
                .build());
    }

    record ProductsServiceProps(Vpc vpc, Cluster cluster, NetworkLoadBalancer networkLoadBalancer,
                                ApplicationLoadBalancer applicationLoadBalancer, Repository repository) {}
}