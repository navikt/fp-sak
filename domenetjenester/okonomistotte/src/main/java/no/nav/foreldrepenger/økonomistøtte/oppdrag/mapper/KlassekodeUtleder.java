package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.InntektskategoriKlassekodeMapper;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.konfig.Environment;

public class KlassekodeUtleder {

    private static final boolean ER_PROD = Environment.current().isProd();
    private static final int FERIEPENGER_NY_MAPPING_OPPTJENINGSÅR = 2023;

    private KlassekodeUtleder() {
    }

    public static KodeKlassifik utled(BeregningsresultatAndel andel, FamilieYtelseType familieYtelseType) {
        var erRefusjonTilArbeidsgiver = !andel.skalTilBrukerEllerPrivatperson();
        return utled(andel.getInntektskategori(), familieYtelseType, erRefusjonTilArbeidsgiver);
    }

    public static KodeKlassifik utled(Inntektskategori inntektskategori, FamilieYtelseType familieYtelseType, boolean refusjonArbeidsgiver) {
        if (refusjonArbeidsgiver) {
            return switch (familieYtelseType) {
                case FØDSEL -> KodeKlassifik.FPF_REFUSJON_AG;
                case ADOPSJON -> KodeKlassifik.FPA_REFUSJON_AG;
                case SVANGERSKAPSPENGER -> KodeKlassifik.SVP_REFUSJON_AG;
            };
        }
        return InntektskategoriKlassekodeMapper.mapTilKlassekode(inntektskategori, familieYtelseType);
    }

    public static KodeKlassifik utledForFeriepenger(FamilieYtelseType familieYtelseType, int opptjeningsår, LocalDate feriepengerDødsdato) {
        // Bruk gammel mapping for opptjeningsår før 2023 + dødsfallstilfelle i 2023 (1 så langt)
        if (opptjeningsår < FERIEPENGER_NY_MAPPING_OPPTJENINGSÅR ||
            (opptjeningsår == FERIEPENGER_NY_MAPPING_OPPTJENINGSÅR && feriepengerDødsdato != null && feriepengerDødsdato.getYear() == FERIEPENGER_NY_MAPPING_OPPTJENINGSÅR)) {
            return KodeKlassifik.FERIEPENGER_BRUKER;
        } else {
            return switch (familieYtelseType) {
                case FØDSEL -> KodeKlassifik.FERIEPENGER_BRUKER;
                case ADOPSJON -> KodeKlassifik.FPA_FERIEPENGER_BRUKER;
                case SVANGERSKAPSPENGER -> KodeKlassifik.SVP_FERIEPENGER_BRUKER;
            };
        }
    }

    public static KodeKlassifik utledForFeriepengeRefusjon(FamilieYtelseType familieYtelseType) {
        return switch (familieYtelseType) {
            case FØDSEL -> KodeKlassifik.FPF_FERIEPENGER_AG;
            case ADOPSJON -> KodeKlassifik.FPA_FERIEPENGER_AG;
            case SVANGERSKAPSPENGER -> KodeKlassifik.SVP_FERIEPENGER_AG;
        };
    }

}
