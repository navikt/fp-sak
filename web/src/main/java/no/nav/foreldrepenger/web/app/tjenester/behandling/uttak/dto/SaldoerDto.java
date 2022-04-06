package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.Map;

import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;

public record SaldoerDto(Map<SaldoVisningStønadskontoType, StønadskontoDto> stonadskontoer, int tapteDagerFpff) {

    public enum SaldoVisningStønadskontoType {
        MØDREKVOTE,
        FEDREKVOTE,
        FELLESPERIODE,
        FORELDREPENGER,
        FORELDREPENGER_FØR_FØDSEL,
        FLERBARNSDAGER,
        UTEN_AKTIVITETSKRAV,
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
