package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

@WebServlet(
        urlPatterns = "/",
        initParams = {
                @WebInitParam(name = "repositoryPath", value = "/media/niko/ExternalHDD/java/javadoc-server-app/repo")
        }
)
public class JarJavadocServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(JarJavadocServlet.class.getName());

    private static final String JAREXTENSION = ".jar";
    private static final String REGEX = "/([a-zA-Z\\.]+)/([a-zA-Z\\-]+)/([a-zA-Z0-9\\.\\-]+)";

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        httpServletResponse.setCharacterEncoding("UTF-8");

        String servletPath = httpServletRequest.getServletPath();
        String contextPath = httpServletRequest.getContextPath();
        String repositoryPath = getInitParameter("repositoryPath");

        LOGGER.info(String.format("Servlet path - %s, Context path - %s", servletPath, contextPath));

        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(servletPath);

        if (matcher.find()) {
            String group = matcher.group();
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);
            LOGGER.info(String.format("Group: %s, groupId: %s, artifactId: %s, version: %s", group, groupId, artifactId, version));

            String context = servletPath.substring(group.length());
            LOGGER.info(String.format("Context: %s", context));

            if ("".equals(context)) {
                String redirectURL = contextPath + servletPath + "/index.html";
                httpServletResponse.sendRedirect(redirectURL);
            } else if ("/".equals(context)) {
                String redirectURL = contextPath + servletPath + "index.html";
                httpServletResponse.sendRedirect(redirectURL);
            } else {
                String jarPath = repositoryPath + "/" + artifactId + "-" + version + "-javadoc" + JAREXTENSION;
                LOGGER.info(String.format("Jar path: %s", jarPath));

                try (JarFile jarFile = new JarFile(jarPath)) {
                    String file = context.substring(1);
                    LOGGER.info(String.format("File in jar: %s", file));

                    ZipEntry entry = jarFile.getEntry(file);
                    if (entry == null) {
                        httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    } else {
                        InputStream inputStream = jarFile.getInputStream(entry);

                        String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

                        PrintWriter printWriter = httpServletResponse.getWriter();
                        printWriter.println(result);
                    }
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
            }
        } else {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
