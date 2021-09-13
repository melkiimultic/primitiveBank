Проект представляет собой простую реализацию банка(очень простую)

## Начальные условия

* maven 3.6.0+
* jdk11
* postgres 11+, с бд 'primitiveBank' и юзером postgres:user поднята на localhost:5432

## Сборка проекта

> ./mvnw clean package

## Swagger: 

[тыц](http://localhost:8080/documentation/swagger-ui/)

## Тестовый пользователь:

| username    | password     | 
|-------------|--------------|
| swaggertest | swaggertest1 |

Этот пользователь подходит как для авторизации в сваггере, так и для взаимодействия с апи

## Авторизация

Получите токен при помощи [auth-controller](http://localhost:8080/documentation/swagger-ui/#/auth-controller)

Авторизуйтесь им в сваггере (Ожидаемый формат - "Bearer $yourcooltoken")