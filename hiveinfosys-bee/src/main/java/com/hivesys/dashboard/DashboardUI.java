package com.hivesys.dashboard;

import java.security.AccessControlException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.hivesys.dashboard.data.DataProvider;
import com.hivesys.dashboard.data.dummy.DummyDataProvider;
import com.hivesys.dashboard.domain.User;
import com.hivesys.dashboard.event.DashboardEventBus;
import com.hivesys.dashboard.event.DashboardEvent.BrowserResizeEvent;
import com.hivesys.dashboard.event.DashboardEvent.CloseOpenWindowsEvent;
import com.hivesys.dashboard.event.DashboardEvent.UserLoggedOutEvent;
import com.hivesys.dashboard.event.DashboardEvent.UserLoginRequestedEvent;
import com.hivesys.dashboard.view.LoginView;
import com.hivesys.dashboard.view.MainView;
import com.google.common.eventbus.Subscribe;
import com.porotype.iconfont.FontAwesome;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.Page;
import com.vaadin.server.Page.BrowserWindowResizeEvent;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

@Theme("dashboard")
@Widgetset("com.hivesys.dashboard.DashboardWidgetSet")
@Title("DataHive Dashboard")
@SuppressWarnings("serial")
public final class DashboardUI extends UI {

    @WebServlet(value = {"/myui/*", "/VAADIN/*"}, asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = com.hivesys.dashboard.DashboardUI.class)

    public static class DashboardServlet extends VaadinServlet {

        @Override
        protected final void servletInitialized() throws ServletException {
            super.servletInitialized();
            getService().addSessionInitListener(new DashboardSessionInitListener());
        }
    }

    /*
     * This field stores an access to the dummy backend layer. In real
     * applications you most likely gain access to your beans trough lookup or
     * injection; and not in the UI but somewhere closer to where they're
     * actually accessed.
     */
    private final DataProvider dataProvider = new DummyDataProvider();
    private final DashboardEventBus dashboardEventbus = new DashboardEventBus();

    @Override
    protected void init(final VaadinRequest request) {
        setLocale(Locale.US);
        
        // initialize the fonts for the plugin
        FontAwesome.load();

        DashboardEventBus.register(this);
        Responsive.makeResponsive(this);
        addStyleName(ValoTheme.UI_WITH_MENU);

        updateContent();

		// Some views need to be aware of browser resize events so a
        // BrowserResizeEvent gets fired to the event bus on every occasion.
        Page.getCurrent().addBrowserWindowResizeListener((final BrowserWindowResizeEvent event) -> {
            DashboardEventBus.post(new BrowserResizeEvent());
        });
       
    }

    /**
     * Updates the correct content for this UI based on the current user status.
     * If the user is logged in with appropriate privileges, main view is shown.
     * Otherwise login view is shown.
     */
    private void updateContent() {
        User user = (User) VaadinSession.getCurrent().getAttribute(
                User.class.getName());
        if (user != null) {
            // Authenticated user
            setContent(new MainView());
            removeStyleName("loginview");
            getNavigator().navigateTo(getNavigator().getState());
        } else {
            setContent(new LoginView());
            addStyleName("loginview");
       }
    }

    @Subscribe
    public void userLoginRequested(final UserLoginRequestedEvent event) {
        User user = null;

        try {
            user = getDataProvider().authenticate(event.getUserName(),
                    event.getPassword());
        } catch (AccessControlException e) {
            Notification notification = new Notification(
                    "Cannot authenticate the user.");
            notification.setDelayMsec(5000);
            notification
                    .setDescription("Either the username or the password is incorrect");
            notification.setHtmlContentAllowed(true);
            notification.setStyleName("tray dark small closable login-help");
            notification.setPosition(Position.MIDDLE_CENTER);
            notification.show(Page.getCurrent());
            return;
        }

        VaadinSession.getCurrent().setAttribute(User.class.getName(), user);
        updateContent();
    }

    @Subscribe
    public void userLoggedOut(final UserLoggedOutEvent event) {
		// When the user logs out, current VaadinSession gets closed and the
        // page gets reloaded on the login screen. Do notice the this doesn't
        // invalidate the current HttpSession.
        VaadinSession.getCurrent().close();
        Page.getCurrent().reload();
    }

    @Subscribe
    public void closeOpenWindows(final CloseOpenWindowsEvent event) {
        for (Window window : getWindows()) {
            window.close();
        }
    }

    /**
     * @return An instance for accessing the (dummy) services layer.
     */
    public static DataProvider getDataProvider() {
        return ((DashboardUI) getCurrent()).dataProvider;
    }

    public static DashboardEventBus getDashboardEventbus() {
        return ((DashboardUI) getCurrent()).dashboardEventbus;
    }
}
