FROM openjdk:8
ADD target/Blog.jar  Blog.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "Blog.jar"]