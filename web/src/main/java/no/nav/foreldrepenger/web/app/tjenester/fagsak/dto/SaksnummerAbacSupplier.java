package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.function.Function;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

public class SaksnummerAbacSupplier {

    public static class Supplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (SaksnummerDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, req.getVerdi());
        }
    }


}



