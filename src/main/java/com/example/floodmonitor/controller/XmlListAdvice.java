package com.example.floodmonitor.controller;

import com.fasterxml.jackson.dataformat.xml.annotation.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.util.List;

@ControllerAdvice
public class XmlListAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        return MappingJackson2XmlHttpMessageConverter.class.isAssignableFrom(converter);
    }
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter method, MediaType type,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof List<?> list)) return body;
        return request.getURI().getPath().endsWith("/stations") ? new Stations(list) : new Measurements(list);
    }
    @JacksonXmlRootElement(localName = "stations")
    public record Stations(@JacksonXmlElementWrapper(useWrapping = false)
                           @JacksonXmlProperty(localName = "station") List<?> items) {}
    @JacksonXmlRootElement(localName = "measurements")
    public record Measurements(@JacksonXmlElementWrapper(useWrapping = false)
                               @JacksonXmlProperty(localName = "measurement") List<?> items) {}
}
