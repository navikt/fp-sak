package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

import java.util.function.Function;

public class BehandlingAbacSuppliers {

    private BehandlingAbacSuppliers() {
    }

    public static class BehandlingIdAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (BehandlingIdDto) obj;
            var abac = AbacDataAttributter.opprett();
             if (req.getBehandlingUuid() != null) {
                abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
            } else {
                throw new IllegalArgumentException("Må ha en av behandlingUuid spesifisert");
            }
            return abac;
        }
    }

    public static class UuidAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (UuidDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
        }
    }

    public static class TaskgruppeAbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
