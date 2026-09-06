package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.ConnectionType;
import software.amazon.awscdk.services.apigateway.Integration;
import software.amazon.awscdk.services.apigateway.IntegrationOptions;
import software.amazon.awscdk.services.apigateway.IntegrationProps;
import software.amazon.awscdk.services.apigateway.IntegrationType;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.apigateway.VpcLink;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.constructs.Construct;

import java.util.Map;

public class ApiStack extends Stack {
    private static final String PRODUCTS_PATH = "/api/products";
    private static final String PRODUCTS_RESOURCE = "products";
    private static final String ID_PATH = "{id}";

    public ApiStack(final Construct scope, final String id, final StackProps props, final ApiStackProps apiStackProps) {
        super(scope, id, props);

        final RestApi restApi = new RestApi(this, "RestApi", RestApiProps.builder()
                .restApiName("ECommerceAPI")
                .build());

        createProductsResource(restApi, apiStackProps);
    }

    private void createProductsResource(
            final RestApi restApi,
            final ApiStackProps apiStackProps) {

        final Resource productsResource = restApi.getRoot().addResource(PRODUCTS_RESOURCE);

        // GET /products
        productsResource.addMethod("GET", createProductsIntegration(apiStackProps, "GET"));

        // POST /products
        productsResource.addMethod("POST", createProductsIntegration(apiStackProps, "POST"));

        final Resource productIdResource = productsResource.addResource(ID_PATH);

        final Map<String, String> integrationParameters = Map.of(
                "integration.request.path.id",
                "method.request.path.id");

        final Map<String, Boolean> methodParameters = Map.of(
                "method.request.path.id",
                true);

        // GET /products/{id}
        productIdResource.addMethod("GET",
                createProductByIdIntegration(apiStackProps, "GET", integrationParameters),
                createMethodOptions(methodParameters));

        // PUT /products/{id}
        productIdResource.addMethod("PUT",
                createProductByIdIntegration(apiStackProps, "PUT", integrationParameters),
                createMethodOptions(methodParameters));

        // DELETE /products/{id}
        productIdResource.addMethod("DELETE",
                createProductByIdIntegration(apiStackProps, "DELETE", integrationParameters),
                createMethodOptions(methodParameters));
    }

    private Integration createProductsIntegration(final ApiStackProps apiStackProps, final String httpMethod) {
        return new Integration(IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod(httpMethod)
                .uri(buildProductsUri(apiStackProps))
                .options(createIntegrationOptions(apiStackProps))
                .build());
    }

    private Integration createProductByIdIntegration(final ApiStackProps apiStackProps, final String httpMethod,
                                                     final Map<String, String> requestParameters) {
        return new Integration(IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod(httpMethod)
                .uri(buildProductByIdUri(apiStackProps))
                .options(createIntegrationOptions(apiStackProps, requestParameters))
                .build());
    }

    private IntegrationOptions createIntegrationOptions(final ApiStackProps apiStackProps) {
        return IntegrationOptions.builder()
                .vpcLink(apiStackProps.vpcLink())
                .connectionType(ConnectionType.VPC_LINK)
                .build();
    }

    private IntegrationOptions createIntegrationOptions(final ApiStackProps apiStackProps,
                                                        final Map<String, String> requestParameters) {
        return IntegrationOptions.builder()
                .vpcLink(apiStackProps.vpcLink())
                .connectionType(ConnectionType.VPC_LINK)
                .requestParameters(requestParameters)
                .build();
    }

    private MethodOptions createMethodOptions(final Map<String, Boolean> requestParameters) {
        return MethodOptions.builder().requestParameters(requestParameters).build();
    }

    private String buildProductsUri(final ApiStackProps apiStackProps) {
        return "http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + ":8080" + PRODUCTS_PATH;
    }

    private String buildProductByIdUri(final ApiStackProps apiStackProps) {
        return buildProductsUri(apiStackProps) + "/{id}";
    }
}

record ApiStackProps(NetworkLoadBalancer networkLoadBalancer, VpcLink vpcLink) {}