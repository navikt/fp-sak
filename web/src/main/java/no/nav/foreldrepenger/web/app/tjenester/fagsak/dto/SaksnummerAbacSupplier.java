package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

import java.util.function.Function;

public class SaksnummerAbacSupplier {

    private SaksnummerAbacSupplier() {
    }

    public static class Supplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (SaksnummerDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, req.getVerdi());
        }
    }


}



