package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import com.myorg.ProductsServiceStack.ProductsServiceProps;

import java.util.HashMap;
import java.util.Map;

public class ECommerceEcsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment environment = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        Map<String, String> infraTags = new HashMap<>();
        infraTags.put("team", "olirrum");
        infraTags.put("project", "ECommerce");
        infraTags.put("environment", "dev");
        infraTags.put("cost", "ECommerceInfra");

        EcrStack ecrStack = new EcrStack(app, "Ecr", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build());

        VpcStack vpcStack = new VpcStack(app, "Vpc", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build());

        ClusterStack clusterStack = new ClusterStack(app, "Cluster", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build(), new ClusterStackProps(vpcStack.getVpc()));
        clusterStack.getNode().addDependency(vpcStack);

        NlbStack nlbStack = new NlbStack(app, "Nlb", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build(), new NlbStackProps(vpcStack.getVpc()));
        nlbStack.getNode().addDependency(vpcStack);

        Map<String, String> productsServiceTags = new HashMap<>();
        infraTags.put("team", "olirrum");
        infraTags.put("project", "ECommerce");
        infraTags.put("environment", "dev");
        infraTags.put("cost", "ProductsService");

        ProductsServiceStack productsServiceStack = new ProductsServiceStack(app, "ProductsService",
                StackProps.builder()
                        .env(environment)
                        .tags(productsServiceTags)
                        .build(),
                new ProductsServiceProps(
                        vpcStack.getVpc(),
                        clusterStack.getCluster(),
                        nlbStack.getNetworkLoadBalancer(),
                        nlbStack.getApplicationLoadBalancer(),
                        ecrStack.getProductsServiceRepository()));
        productsServiceStack.getNode().addDependency(vpcStack);
        productsServiceStack.getNode().addDependency(clusterStack);
        productsServiceStack.getNode().addDependency(nlbStack);
        productsServiceStack.getNode().addDependency(ecrStack);

        app.synth();
    }
}