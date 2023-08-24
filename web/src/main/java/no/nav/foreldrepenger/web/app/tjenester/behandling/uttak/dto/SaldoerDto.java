package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;

import java.util.Map;

public record SaldoerDto(Map<SaldoVisningStønadskontoType, StønadskontoDto> stonadskontoer, int tapteDagerFpff) {

    public enum SaldoVisningStønadskontoType {
        MØDREKVOTE,
        FEDREKVOTE,
        FELLESPERIODE,
        FORELDREPENGER,
        FORELDREPENGER_FØR_FØDSEL,
        FLERBARNSDAGER,
        UTEN_AKTIVITETSKRAV,
        MINSTERETT_NESTE_STØNADSPERIODE,
        MINSTERETT;

        public static SaldoVisningStønadskontoType fra(Stønadskontotype stønadskontotype) {
            return switch (stønadskontotype) {
                case FORELDREPENGER -> FORELDREPENGER;
                case MØDREKVOTE -> MØDREKVOTE;
                case FORELDREPENGER_FØR_FØDSEL -> FORELDREPENGER_FØR_FØDSEL;
                case FELLESPERIODE -> FELLESPERIODE;
                case FEDREKVOTE -> FEDREKVOTE;
            };
        }
    }

}
