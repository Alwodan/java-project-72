package hexlet.code.controllers;

import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;

import java.net.URL;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import hexlet.code.domain.Url;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class UrlController {

    public static Handler createUrl = ctx -> {
        String parsedUrl = ctx.formParam("url");
        StringBuilder resultUrl = new StringBuilder();
        URL url;

        try {
            url = new URL(parsedUrl);
            if (url.getHost().isEmpty()) {
                throw new Exception();
            }
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }

        resultUrl.append(url.getProtocol()).append("://").append(url.getHost());
        if (url.getPort() != -1) {
            resultUrl.append(":").append(url.getPort());
        }
        Url existing = new QUrl().name.equalTo(resultUrl.toString()).findOne();

        if (existing != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "info");
            ctx.redirect("/urls");
        } else {
            Url toAdd = new Url(resultUrl.toString());
            toAdd.save();
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
            ctx.redirect("/urls");
        }
    };

    public static Handler showAllUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };

    public static Handler showUrlById = ctx -> {
        Long parsedId = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl().id.equalTo(parsedId).checks.fetch().orderBy().checks.createdAt.desc().findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        ctx.attribute("url", url);
        ctx.render("/urls/show.html");
    };

    public static Handler performCheck = ctx -> {
        Long parsedId = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        Url url = new QUrl().id.equalTo(parsedId).findOne();
        if (url == null) {
            throw new NotFoundResponse();
        }

        try {
            HttpResponse<String> response = Unirest.get(url.getName()).asString();
            Document document = Jsoup.parse(response.getBody());

            Element h1Raw = document.selectFirst("h1");
            Element descriptionRaw = document.selectFirst("meta[name=description]");

            int statusCode = response.getStatus();
            String title = document.title();
            String h1 = h1Raw == null ? "" : h1Raw.text();
            String description = descriptionRaw == null ? "" : descriptionRaw.attr("content");

            UrlCheck check = new UrlCheck(statusCode, title, h1, description, url);
            url.getChecks().add(check);
            url.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls/" + parsedId);
    };
}
