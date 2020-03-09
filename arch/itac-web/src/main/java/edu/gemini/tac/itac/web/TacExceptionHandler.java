package edu.gemini.tac.itac.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ddawson
 * Date: 5/21/11
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class TacExceptionHandler extends SimpleMappingExceptionResolver {
    @Resource(name ="errorMap")
    Map<String, Integer> errorMap;

    @Resource(name = "itacProperties")
    protected Properties itacProperties;

//    @Value("${itac.version}")
//	private String VERSION;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ModelAndView modelAndView = new ModelAndView();

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter);
        ex.printStackTrace(writer);
        final String errorString = stringWriter.toString();
        logger.error(errorString);
        Integer currentValue = errorMap.get(errorString);

        if (currentValue == null) {
            currentValue = Integer.valueOf(0);
        }
        errorMap.put(errorString, Integer.valueOf(currentValue + 1));

        modelAndView.setViewName("error");

        final SortedSet<String> errors  = new TreeSet<String>();
        for (Map.Entry<String,Integer> entry : errorMap.entrySet()) {
            errors.add(String.format("%08d", entry.getValue()) + entry.getKey());
        }

        modelAndView.addObject("errorMap", errorMap.toString());
        modelAndView.addObject("errors", errors.toString());
        modelAndView.addObject("exception", errorString.toString());
//        modelAndView.addObject("version", VERSION);

        return modelAndView;
    }
}
