package no.nav.foreldrepenger.web.app.rest;

import javax.enterprise.context.ApplicationScoped;

import no.nav.vedtak.sikkerhet.ContextPathHolder;

@ApplicationScoped
class ContextPathFromCphProvider implements ContextPathProvider {
    @Override
    public String get() {
        return ContextPathHolder.instance().getContextPath();
    }
}
