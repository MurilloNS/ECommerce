# ☁️ Spring Boot 3 & AWS Cloud-Native Microservices

Aplicação em arquitetura de microsserviços orientada a eventos (*Event-Driven Architecture*) desenvolvida em **Java 21** e **Spring Boot 3**, utilizando infraestrutura como código (IaC) com **AWS CDK v2** e orquestração de containers serverless com **AWS ECS & Fargate**.

---

## 📌 Visão Geral do Projeto

O objetivo deste projeto é demonstrar a construção, provisionamento e observabilidade de ecossistemas distribuídos e escaláveis na nuvem Amazon Web Services (AWS), aplicando boas práticas de desenvolvimento backend, conteinerização e automação de infraestrutura.

### 🛠️ Principais Recursos e Funcionalidades

- **Orquestração Serverless de Containers:** Implantação de microsserviços via AWS ECS no modo Fargate.
- **Infraestrutura como Código (IaC):** Provisionamento declarativo de recursos na AWS em Java através do AWS CDK v2.
- **Comunicação Assíncrona & Mensageria:** Padrão Pub/Sub e Filas com AWS SNS, SQS e mecanismo de *Dead-Letter Queue* (DLQ).
- **Processamento Baseado em Eventos (Event-Driven):** Integração do AWS S3 para gatilhos automatizados de processamento de arquivos via SQS.
- **Persistência NoSQL:** Modelagem e consultas dinâmicas no AWS DynamoDB utilizando *Composite Primary Key* e SDK v2.
- **Segurança e Rede:** Configuração de VPC, Subnets privadas/públicas, Security Groups e gerenciamento de permissões granulares com AWS IAM.
- **Roteamento e Gateway:** Exposição de APIs com AWS API Gateway e balanceamento de carga com Application Load Balancer (ALB) e Target Groups.
- **Observabilidade e Monitoramento:** Rastreamento distribuído com AWS X-Ray, análise centralizada de logs com CloudWatch Insights/Log4j2 em JSON e criação de CloudWatch Alarms.

---

## 🚀 Tecnologias e Ferramentas Utilizadas

| Categoria | Tecnologia / Serviço AWS |
| :--- | :--- |
| **Linguagem & Framework** | Java 21, Spring Boot 3 |
| **Infraestrutura como Código** | AWS CDK v2 (Java) |
| **Computação & Containers** | AWS ECS, AWS Fargate, AWS ECR, Docker |
| **Banco de Dados & Storage** | AWS DynamoDB (NoSQL), AWS S3 |
| **Mensageria & Eventos** | AWS SNS, AWS SQS (com DLQ) |
| **Redes & Roteamento** | AWS VPC, API Gateway REST, Application Load Balancer (ALB) |
| **Observabilidade & Gestão** | AWS CloudWatch (Logs, Alarms, Container Insights), AWS X-Ray, AWS IAM |

---

## 🏗️ Arquitetura do Sistema

```text
[ API Gateway / ALB ] 
        │
        ▼
 [ AWS ECS (Fargate) ] ◄──► [ AWS DynamoDB ]
        │
        ├──► [ AWS SNS ] ──► [ AWS SQS ] ──► [ Consumer Microservice ]
        │
        └──► [ AWS S3 ] ──► [ Event Notification ] ──► [ AWS SQS ]
```

## 🔧 Como Executar o Projeto

### Pré-requisitos
  - Java 21 e Maven instalados.
  - Docker em execução.
  - AWS CLI configurado com credenciais válidas.
  - AWS CDK v2 instalado globalmente (npm install -g aws-cdk).

### Passo a Passo

1. Clonar o repositório:
```
git clone https://github.com/MurilloNS/seu-repositorio.git
cd seu-repositorio
```

2. Compilar a aplicação:
```
mvn clean package
```

3. Sintetizar a stack com AWS CDK (IaC):
```
cdk synth
```

4. Fazer o deploy da infraestrutura na AWS:
```
cdk deploy --all
```

### 📈 Tópicos de Aprendizado & Boas Práticas
  - Utilização do AWS SDK v2 para Java otimizado para chamadas assíncronas e concorrentes.
  - Tratamento e isolamento de falhas de mensagens através de Dead-Letter Queues (DLQ) no SQS.
  - Rastreamento e diagnóstico de microsserviços distribuídos com AWS X-Ray.
  - Práticas de gestão de custos na AWS utilizando AWS Cost Explorer e marcação de recursos via Resource Tags.
