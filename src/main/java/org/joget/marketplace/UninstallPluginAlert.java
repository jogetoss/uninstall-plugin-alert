package org.joget.marketplace;

import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.UiHtmlInjectorPluginAbstract;
import org.joget.apps.app.service.AppDevUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeansException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;

public class UninstallPluginAlert extends UiHtmlInjectorPluginAbstract implements PluginWebSupport {
       
    @Override
    public String getName() {
        return "Uninstall Plugin Alert";
    }

    @Override
    public String getVersion() {
        return Activator.getBundleVersion();
    }

    @Override
    public String getDescription() {
        return "UI Html Injector plugin to alert users the plugin is being used in which app before uninstalling.";
    }

    @Override
    public String[] getInjectUrlPatterns() {
        return new String[] {"/web/console/setting/plugin"};
    }
    
    @Override
    public String getHtml(HttpServletRequest request) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map data = new HashMap();
        data.put("plugin", this);
        data.put("request", request);
        
        return pluginManager.getPluginFreeMarkerTemplate(data, getClassName(), "/templates/UninstallPluginAlertUiHtmlInjector.ftl", null);
    }

    @Override
    public boolean isIncludeForAjaxThemePageSwitching() {
        return false;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String body = request.getReader().lines().collect(Collectors.joining());
        JSONObject json = new JSONObject(body);
        JSONArray pluginClasses = json.getJSONArray("selectedList");
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        
        Set<Map<String, String>> apps = new HashSet<>();
        Set<String> jarFiles = new HashSet<>();
        for (Object p: pluginClasses.toList()) {
            String jar = pluginManager.getJarFileName(p.toString());
            if (jar != null) {
                jarFiles.add(StringUtil.unescapeString(jar, StringUtil.TYPE_URL));
            }
        }
        LogUtil.info(getClassName(), "Plugins stored in wflow/app_plugins are " + jarFiles.toString());

        // get appid and appname from published apps that use the plugin jars (get list of jar files frommsame logic that export app uses)
        // note that if there are jar files in app_src/<appid>/<app_id>_<app_version>/plugins, even if it is unused it will be detected here
        apps = getPublishedApps(jarFiles);

        List<String> appIds = new ArrayList<>();
        List<String> appNames = new ArrayList<>();
        List<String> appVersions = new ArrayList<>();

        for (Map<String, String> map : apps) {
            appIds.add(map.get("id"));
            appNames.add(map.get("name"));
            appVersions.add(map.get("version"));
        }

        Map<String, Object> jsonOutput = new HashMap<>();
        jsonOutput.put("ids", appIds);
        jsonOutput.put("names", appNames);
        jsonOutput.put("versions", appVersions);
        jsonOutput.put("jars", jarFiles);

        PrintWriter out = response.getWriter();
        out.print(new ObjectMapper().writeValueAsString(jsonOutput)); // use Jackson for proper JSON
        out.flush();
    }

    public Set<Map<String, String>> getPublishedApps(Set<String> jarFiles) {
        Set<Map<String, String>> apps = new HashSet<>();
        Set<String> normalizedJarFiles = jarFiles.stream()
                .map(UninstallPluginAlert::normalizeJarName)
                .collect(Collectors.toSet());

        try {
            // Use the core DAO instead of hand-rolled JDBC against app_app: same
            // "published = true" predicate, but it (a) returns full AppDefinition
            // entities directly (no separate appService.getAppDefinition lookup
            // per row needed), and (b) goes through Joget's DynamicDataSource
            // routing rather than grabbing setupDataSource directly, which is the
            // setup/profile datasource and not necessarily the tenant's app
            // datasource. Same pattern core itself uses for this exact "scan
            // published apps for plugin usage" case, e.g. MissingPluginCheck.
            AppDefinitionDao appDefinitionDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
            Collection<AppDefinition> publishedApps = appDefinitionDao.findPublishedApps("name", Boolean.FALSE, null, null);

            for (AppDefinition appDef : publishedApps) {
                String appId = appDef.getAppId();
                String appName = appDef.getName();
                String appVersion = String.valueOf(appDef.getVersion());
                Collection<String> appPlugins = AppDevUtil.getPluginJarList(appDef);
                LogUtil.info(getClassName(), appId + " plugins are " + appPlugins.toString());
                // check for matches without looking at jar file versions
                boolean hasMatch = appPlugins.stream()
                            .map(UninstallPluginAlert::normalizeJarName)
                            .anyMatch(normalizedJarFiles::contains);
                if (hasMatch) {
                    Map<String, String> appInfo = new HashMap<>();
                    appInfo.put("id", appId);
                    appInfo.put("name", appName);
                    appInfo.put("version", appVersion);
                    apps.add(appInfo);
                }
            }

        } catch (BeansException e) {
            LogUtil.error(getClassName(), e, "");
        }

        return apps;
    }

    // Windows duplicate-download suffix, e.g. "my-plugin-1.0.0 (1).jar"
    private static final Pattern COPY_SUFFIX = Pattern.compile("\\s*\\(\\d+\\)$");
    // First "-<version>" boundary in a Maven-style "artifactId-version.jar" filename.
    // Matched from the leftmost occurrence so trailing pre-release qualifiers such as
    // "-1.0.0-RC1", "-2.0-SNAPSHOT" or "-v2.0.1-beta2" are stripped in full, rather than
    // only when they match a fixed set of suffix patterns.
    private static final Pattern VERSION_SUFFIX = Pattern.compile("-v?\\d.*$");

    private static String normalizeJarName(String filename) {
        if (filename == null) {
            return "";
        }
        filename = filename.trim();
        if (filename.regionMatches(true, filename.length() - 4, ".jar", 0, 4)) {
            filename = filename.substring(0, filename.length() - 4);
        }
        filename = COPY_SUFFIX.matcher(filename).replaceAll("");
        filename = VERSION_SUFFIX.matcher(filename).replaceFirst("");
        return filename;
    }

}
