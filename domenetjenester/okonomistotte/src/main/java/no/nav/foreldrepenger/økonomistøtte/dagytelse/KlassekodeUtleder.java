package no.nav.foreldrepenger.økonomistøtte.dagytelse;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.InntektskategoriKlassekodeMapper;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;

public class KlassekodeUtleder {

    private KlassekodeUtleder() {
    }

    public static KodeKlassifik utled(TilkjentYtelseAndel andel) {
        FamilieYtelseType familieYtelseType = andel.getFamilieYtelseType();
        if (andel.skalTilBrukerEllerPrivatperson()) {
            return InntektskategoriKlassekodeMapper.mapTilKlassekode(andel.getInntektskategori(), familieYtelseType);
        } else {
            switch (familieYtelseType) {
                case FØDSEL:
                    return KodeKlassifik.FPF_REFUSJON_AG;
                case ADOPSJON:
                    return KodeKlassifik.FPA_REFUSJON_AG;
                case SVANGERSKAPSPENGER:
                    return KodeKlassifik.SVP_REFUSJON_AG;
                default:
                    throw new IllegalArgumentException("Utvikler feil: Opdrag andel har ikke-støttet familie ytelse type: " + familieYtelseType);
            }
        }
    }

    public static KodeKlassifik utledForFeriepenger(Oppdragsmottaker mottaker, FamilieYtelseType familieYtelseType) {
        if (mottaker.erBruker()) {
            return KodeKlassifik.FERIEPENGER_BRUKER;
        } else {
            switch (familieYtelseType) {
                case FØDSEL:
                    return KodeKlassifik.FPF_FERIEPENGER_AG;
                case ADOPSJON:
                    return KodeKlassifik.FPA_FERIEPENGER_AG;
                case SVANGERSKAPSPENGER:
                    return KodeKlassifik.SVP_FERIEPENGER_AG;
                default:
                    throw new IllegalArgumentException("Utvikler feil: Ikke-støttet familie ytelse type: " + familieYtelseType);
            }
        }
    }

    public static List<KodeKlassifik> getKlassekodeListe(List<TilkjentYtelseAndel> andelListe) {
        return andelListe.stream()
            .map(KlassekodeUtleder::utled)
            .distinct()
            .collect(Collectors.toList());
    }
}
