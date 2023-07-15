package hexlet.code.controllers;

import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import java.net.URL;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import hexlet.code.domain.Url;
import io.javalin.http.NotFoundResponse;

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

    public static Handler showUrl = ctx -> {
        Long parsedId = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl().id.equalTo(parsedId).findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        ctx.attribute("url", url);
        ctx.render("/urls/show.html");
    };
}
