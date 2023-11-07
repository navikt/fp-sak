package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;

final class RettighetUtleder {

    private RettighetUtleder() {}

    static RettighetType utledRettighet(ForeldrepengerUttakPeriode foreldrepengerUttakPeriode, YtelseFordelingAggregat ytelseFordelingAggregat, Set<Stønadskonto> konti) {
        var rettighetFraPeriodeResultat = rettighetFraPeriodeResultat(foreldrepengerUttakPeriode);

        if (rettighetFraPeriodeResultat != null) {
            return rettighetFraPeriodeResultat;
        }
        var rettighetFraKonto = rettighetFraKonto(konti);
        if (rettighetFraKonto != null) {
            return rettighetFraKonto;
        }
        return rettighetFraYf(ytelseFordelingAggregat);
    }

    private static RettighetType rettighetFraKonto(Set<Stønadskonto> konti) {
        return konti.stream().anyMatch(k -> k.getStønadskontoType().tilhørerTredeling()) ? RettighetType.BEGGE_RETT : null;
    }

    private static RettighetType rettighetFraYf(YtelseFordelingAggregat ytelseFordelingAggregat) {
        if (UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat)) {
            return RettighetType.ALENEOMSORG;
        }
        if (UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat, Optional.empty())) { //TODO
            return RettighetType.BEGGE_RETT;
        }
        return RettighetType.BARE_SØKER_RETT;
    }

    private static RettighetType rettighetFraPeriodeResultat(ForeldrepengerUttakPeriode foreldrepengerUttakPeriode) {
        return switch (foreldrepengerUttakPeriode.getResultatÅrsak()) {

            case UKJENT -> throw new IllegalStateException("Ukjent resultatårsak");
            case FELLESPERIODE_ELLER_FORELDREPENGER -> RettighetType.BEGGE_RETT;
            case KVOTE_ELLER_OVERFØRT_KVOTE -> RettighetType.BEGGE_RETT;
            case FORELDREPENGER_KUN_FAR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case FORELDREPENGER_ALENEOMSORG -> RettighetType.ALENEOMSORG;
            case INNVILGET_FORELDREPENGER_FØR_FØDSEL -> null;
            case FORELDREPENGER_KUN_MOR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case UTSETTELSE_GYLDIG_PGA_FERIE -> null;
            case UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID -> null;
            case UTSETTELSE_GYLDIG_PGA_INNLEGGELSE -> null;
            case UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT -> null;
            case UTSETTELSE_GYLDIG_PGA_SYKDOM -> null;
            case UTSETTELSE_GYLDIG_PGA_FERIE_KUN_FAR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case UTSETTELSE_GYLDIG_PGA_ARBEID_KUN_FAR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case UTSETTELSE_GYLDIG_PGA_SYKDOM_KUN_FAR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case UTSETTELSE_GYLDIG_PGA_INNLEGGELSE_KUN_FAR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT_KUN_FAR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER -> RettighetType.BARE_SØKER_RETT;
            case OVERFØRING_ANNEN_PART_SYKDOM_SKADE -> null;
            case OVERFØRING_ANNEN_PART_INNLAGT -> null;
            case OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET -> RettighetType.ALENEOMSORG;
            case UTSETTELSE_GYLDIG -> null;
            case UTSETTELSE_GYLDIG_SEKS_UKER_INNLEGGELSE -> null;
            case UTSETTELSE_GYLDIG_SEKS_UKER_FRI_BARN_INNLAGT -> null;
            case UTSETTELSE_GYLDIG_SEKS_UKER_FRI_SYKDOM -> null;
            case UTSETTELSE_GYLDIG_BFR_AKT_KRAV_OPPFYLT -> RettighetType.BARE_SØKER_RETT;
            case GRADERING_FELLESPERIODE_ELLER_FORELDREPENGER -> RettighetType.BEGGE_RETT;
            case GRADERING_KVOTE_ELLER_OVERFØRT_KVOTE -> RettighetType.BEGGE_RETT;
            case GRADERING_ALENEOMSORG -> RettighetType.ALENEOMSORG;
            case GRADERING_FORELDREPENGER_KUN_FAR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case GRADERING_FORELDREPENGER_KUN_MOR_HAR_RETT -> RettighetType.BARE_SØKER_RETT;
            case GRADERING_KUN_FAR_HAR_RETT_MOR_UFØR -> RettighetType.BARE_SØKER_RETT;
            case FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR -> RettighetType.BARE_SØKER_RETT;
            case FORELDREPENGER_FELLESPERIODE_TIL_FAR -> RettighetType.BEGGE_RETT;
            case FORELDREPENGER_REDUSERT_GRAD_PGA_SAMTIDIG_UTTAK -> RettighetType.BEGGE_RETT;
            case MSP_INNVILGET_FØRSTE_6_UKENE -> RettighetType.BEGGE_RETT;


            //Ikkeoppfylt
            case IKKE_STØNADSDAGER_IGJEN -> null;
            case MOR_HAR_IKKE_OMSORG -> null;
            case HULL_MELLOM_FORELDRENES_PERIODER -> null;
            case DEN_ANDRE_PART_SYK_SKADET_IKKE_OPPFYLT -> null;
            case DEN_ANDRE_PART_INNLEGGELSE_IKKE_OPPFYLT -> null;
            case FAR_HAR_IKKE_OMSORG -> null;
            case MOR_SØKER_FELLESPERIODE_FØR_12_UKER_FØR_TERMIN_FØDSEL -> RettighetType.BEGGE_RETT;
            case SØKNADSFRIST -> null;
            case BARN_OVER_3_ÅR -> null;
            case ARBEIDER_I_UTTAKSPERIODEN_MER_ENN_0_PROSENT -> null;
            case AVSLAG_GRADERING_ARBEIDER_100_PROSENT_ELLER_MER -> null;
            case UTSETTELSE_FØR_TERMIN_FØDSEL -> null;
            case UTSETTELSE_INNENFOR_DE_FØRSTE_6_UKENE -> null;
            case FERIE_SELVSTENDIG_NÆRINGSDRIVENDSE_FRILANSER -> null;
            case IKKE_LOVBESTEMT_FERIE -> null;
            case INGEN_STØNADSDAGER_IGJEN -> null;
            case BARE_FAR_RETT_MOR_FYLLES_IKKE_AKTIVITETSKRAVET -> RettighetType.BARE_SØKER_RETT;
            case IKKE_HELTIDSARBEID -> null;
            case SØKERS_SYKDOM_SKADE_IKKE_OPPFYLT -> null;
            case SØKERS_INNLEGGELSE_IKKE_OPPFYLT -> null;
            case BARNETS_INNLEGGELSE_IKKE_OPPFYLT -> null;
            case UTSETTELSE_FERIE_PÅ_BEVEGELIG_HELLIGDAG -> null;
            case AKTIVITETSKRAVET_ARBEID_IKKE_OPPFYLT -> null;
            case AKTIVITETSKRAVET_OFFENTLIG_GODKJENT_UTDANNING_IKKE_OPPFYLT -> null;
            case AKTIVITETSKRAVET_OFFENTLIG_GODKJENT_UTDANNING_I_KOMBINASJON_MED_ARBEID_IKKE_OPPFYLT -> null;
            case AKTIVITETSKRAVET_MORS_SYKDOM_IKKE_OPPFYLT -> null;
            case AKTIVITETSKRAVET_MORS_INNLEGGELSE_IKKE_OPPFYLT -> null;
            case AKTIVITETSKRAVET_MORS_DELTAKELSE_PÅ_INTRODUKSJONSPROGRAM_IKKE_OPPFYLT -> null;
            case AKTIVITETSKRAVET_MORS_DELTAKELSE_PÅ_KVALIFISERINGSPROGRAM_IKKE_OPPFYLT -> null;
            case MORS_MOTTAK_AV_UFØRETRYGD_IKKE_OPPFYLT -> RettighetType.BARE_SØKER_RETT;
            case STEBARNSADOPSJON_IKKE_NOK_DAGER -> null;
            case FLERBARNSFØDSEL_IKKE_NOK_DAGER -> null;
            case SAMTIDIG_UTTAK_IKKE_GYLDIG_KOMBINASJON -> RettighetType.BEGGE_RETT;
            case UTSETTELSE_FERIE_IKKE_DOKUMENTERT -> null;
            case UTSETTELSE_ARBEID_IKKE_DOKUMENTERT -> null;
            case UTSETTELSE_SØKERS_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT -> null;
            case UTSETTELSE_SØKERS_INNLEGGELSE_IKKE_DOKUMENTERT -> null;
            case UTSETTELSE_BARNETS_INNLEGGELSE_IKKE_DOKUMENTERT -> null;
            case AKTIVITETSKRAVET_ARBEID_IKKE_DOKUMENTERT -> null;
            case AKTIVITETSKRAVET_UTDANNING_IKKE_DOKUMENTERT -> null;
            case AKTIVITETSKRAVET_ARBEID_I_KOMB_UTDANNING_IKKE_DOKUMENTERT -> null;
            case AKTIVITETSKRAVET_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT -> null;
            case AKTIVITETSKRAVET_INNLEGGELSE_IKKE_DOKUMENTERT -> null;
            case SØKER_ER_DØD -> null;
            case BARNET_ER_DØD -> null;
            case MOR_IKKE_RETT_TIL_FORELDREPENGER -> RettighetType.BARE_SØKER_RETT;
            case SYKDOM_SKADE_INNLEGGELSE_IKKE_DOKUMENTERT -> null;
            case FAR_IKKE_RETT_PÅ_FELLESPERIODE_FORDI_MOR_IKKE_RETT -> RettighetType.BARE_SØKER_RETT;
            case ANNEN_FORELDER_HAR_RETT -> RettighetType.BEGGE_RETT;
            case FRATREKK_PLEIEPENGER -> null;
            case AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID -> null;
            case AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID -> null;
            case DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK -> RettighetType.BEGGE_RETT;
            case IKKE_SAMTYKKE_MELLOM_PARTENE -> null;
            case DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE -> RettighetType.BEGGE_RETT;
            case OPPHØR_MEDLEMSKAP -> null;
            case AKTIVITETSKRAVET_INTROPROGRAM_IKKE_DOKUMENTERT -> null;
            case AKTIVITETSKRAVET_KVP_IKKE_DOKUMENTERT -> null;
            case HAR_IKKE_ALENEOMSORG_FOR_BARNET -> null;
            case AVSLAG_GRADERING_SØKER_ER_IKKE_I_ARBEID -> null;
            case MOR_TAR_IKKE_ALLE_UKENE -> null;
            case FØDSELSVILKÅRET_IKKE_OPPFYLT -> null;
            case ADOPSJONSVILKÅRET_IKKE_OPPFYLT -> null;
            case FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT -> null;
            case OPPTJENINGSVILKÅRET_IKKE_OPPFYLT -> null;
            case UTTAK_FØR_OMSORGSOVERTAKELSE -> null;
            case BARE_FAR_RETT_IKKE_SØKT -> RettighetType.BARE_SØKER_RETT;
            case MOR_FØRSTE_SEKS_UKER_IKKE_SØKT -> null;
            case STØNADSPERIODE_NYTT_BARN -> null;
            case FAR_SØKT_FØR_FØDSEL -> null;
            case FAR_MER_ENN_TI_DAGER_FEDREKVOTE_IFM_FØDSEL -> RettighetType.BEGGE_RETT;
            case BARE_FAR_RETT_MANGLER_MORS_AKTIVITET -> RettighetType.BARE_SØKER_RETT;
            case SØKERS_SYKDOM_SKADE_SEKS_UKER_IKKE_OPPFYLT -> null;
            case SØKERS_INNLEGGELSE_SEKS_UKER_IKKE_OPPFYLT -> null;
            case BARNETS_INNLEGGELSE_SEKS_UKER_IKKE_OPPFYLT -> null;
            case SØKERS_SYKDOM_ELLER_SKADE_SEKS_UKER_IKKE_DOKUMENTERT -> null;
            case SØKERS_INNLEGGELSE_SEKS_UKER_IKKE_DOKUMENTERT -> null;
            case BARNETS_INNLEGGELSE_SEKS_UKER_IKKE_DOKUMENTERT -> null;
        };
    }
}
