package hexlet.code.controllers;

import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import java.net.URL;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import hexlet.code.domain.Url;

public class UrlController {

    public static Handler createUrl = ctx -> {
        String parsedUrl = ctx.formParam("url");
        StringBuilder resultUrl = new StringBuilder();

        try {
            URL url = new URL(parsedUrl);
            resultUrl.append(url.getProtocol()).append(url.getHost());
            if (url.getPort() != -1) {
                resultUrl.append(":").append(url.getPort());
            }
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.render("/");
            return;
        }
        Url existing = new QUrl().name.equalTo(resultUrl.toString()).findOne();

        if (existing != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "info");
            ctx.redirect("/urls");
        } else {
            Url toAdd = new Url(resultUrl.toString());
            toAdd.save();
            ctx.sessionAttribute("flash", "Статья успешно создана");
            ctx.sessionAttribute("flash-type", "success");
            ctx.redirect("/urls");
        }
    };

    public static Handler showUrls = ctx -> {
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
}
