package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.InntektskategoriKlassekodeMapper;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;

public class KlassekodeUtleder {

    private KlassekodeUtleder() {
    }

    public static String utled(TilkjentYtelseAndel andel) {
        FamilieYtelseType familieYtelseType = andel.getFamilieYtelseType();
        if (andel.skalTilBrukerEllerPrivatperson()) {
            return InntektskategoriKlassekodeMapper.mapTilKlassekode(andel.getInntektskategori(), familieYtelseType);
        } else {
            switch (familieYtelseType) {
                case FØDSEL:
                    return ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik();
                case ADOPSJON:
                    return ØkonomiKodeKlassifik.FPADREFAG_IOP.getKodeKlassifik();
                case SVANGERSKAPSPENGER:
                    return ØkonomiKodeKlassifik.FPSVREFAG_IOP.getKodeKlassifik();
                default:
                    throw new IllegalArgumentException("Utvikler feil: Opdrag andel har ikke-støttet familie ytelse type: " + familieYtelseType);
            }
        }
    }

    public static String utledForFeriepenger(Oppdragsmottaker mottaker, FamilieYtelseType familieYtelseType) {
        if (mottaker.erBruker()) {
            return ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik();
        } else {
            switch (familieYtelseType) {
                case FØDSEL:
                    return ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik();
                case ADOPSJON:
                    return ØkonomiKodeKlassifik.FPADREFAGFER_IOP.getKodeKlassifik();
                case SVANGERSKAPSPENGER:
                    return ØkonomiKodeKlassifik.FPSVREFAGFER_IOP.getKodeKlassifik();
                default:
                    throw new IllegalArgumentException("Utvikler feil: Ikke-støttet familie ytelse type: " + familieYtelseType);
            }
        }
    }

    public static List<String> getKlassekodeListe(List<TilkjentYtelseAndel> andelListe) {
        return andelListe.stream()
            .map(KlassekodeUtleder::utled)
            .distinct()
            .collect(Collectors.toList());
    }
}
