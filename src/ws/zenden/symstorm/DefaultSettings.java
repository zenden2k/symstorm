package ws.zenden.symstorm;

public class DefaultSettings {
    enum RouteFindMethod  {
        USING_URL_MATCHER_FILE, USING_ROUTE_MATCH, USING_ROUTE_DUMP
    }
    public final static String appPath = "app/";
    public final static RouteFindMethod routeFindMethod = RouteFindMethod.USING_ROUTE_DUMP;
    public final static String hosts = "";
}
