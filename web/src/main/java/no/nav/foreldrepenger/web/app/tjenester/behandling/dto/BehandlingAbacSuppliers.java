package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.function.Function;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

public class BehandlingAbacSuppliers {

    public static class BehandlingIdAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (BehandlingIdDto) obj;
            var abac = AbacDataAttributter.opprett();
            if (req.getBehandlingId() != null) {
                abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, req.getBehandlingId());
            } else if (req.getBehandlingUuid() != null) {
                abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
            } else {
                throw new IllegalArgumentException("Må ha en av behandlingId/behandlingUuid spesifisert");
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
