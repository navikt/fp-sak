package no.nav.foreldrepenger.dokumentbestiller.formidling;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.kontrakter.formidling.kodeverk.DokumentMal;
import no.nav.foreldrepenger.kontrakter.formidling.kodeverk.RevurderingÅrsak;

public class BestillDokumentDtoMapper {

    private BestillDokumentDtoMapper() {
    }

    public static DokumentMal mapDokumentMal(DokumentMalType bestillingDokumentMal) {
        return switch (bestillingDokumentMal) {
            case FRITEKSTBREV -> DokumentMal.FRITEKSTBREV;
            case VEDTAKSBREV_FRITEKST_HTML -> DokumentMal.FRITEKSTBREV_HTML;
            case ENGANGSSTØNAD_INNVILGELSE -> DokumentMal.ENGANGSSTØNAD_INNVILGELSE;
            case ENGANGSSTØNAD_AVSLAG -> DokumentMal.ENGANGSSTØNAD_AVSLAG;
            case FORELDREPENGER_INNVILGELSE -> DokumentMal.FORELDREPENGER_INNVILGELSE;
            case FORELDREPENGER_AVSLAG -> DokumentMal.FORELDREPENGER_AVSLAG;
            case FORELDREPENGER_OPPHØR -> DokumentMal.FORELDREPENGER_OPPHØR;
            case FORELDREPENGER_ANNULLERT -> DokumentMal.FORELDREPENGER_ANNULLERT;
            case FORELDREPENGER_INFO_TIL_ANNEN_FORELDER -> DokumentMal.FORELDREPENGER_INFO_TIL_ANNEN_FORELDER;
            case SVANGERSKAPSPENGER_INNVILGELSE -> DokumentMal.SVANGERSKAPSPENGER_INNVILGELSE;
            case SVANGERSKAPSPENGER_OPPHØR -> DokumentMal.SVANGERSKAPSPENGER_OPPHØR;
            case SVANGERSKAPSPENGER_AVSLAG -> DokumentMal.SVANGERSKAPSPENGER_AVSLAG;
            case INNHENTE_OPPLYSNINGER -> DokumentMal.INNHENTE_OPPLYSNINGER;
            case VARSEL_OM_REVURDERING -> DokumentMal.VARSEL_OM_REVURDERING;
            case INFO_OM_HENLEGGELSE -> DokumentMal.INFO_OM_HENLEGGELSE;
            case INNSYN_SVAR -> DokumentMal.INNSYN_SVAR;
            case IKKE_SØKT -> DokumentMal.IKKE_SØKT;
            case INGEN_ENDRING -> DokumentMal.INGEN_ENDRING;
            case FORLENGET_SAKSBEHANDLINGSTID -> DokumentMal.FORLENGET_SAKSBEHANDLINGSTID;
            case FORLENGET_SAKSBEHANDLINGSTID_MEDL -> DokumentMal.FORLENGET_SAKSBEHANDLINGSTID_MEDL;
            case FORLENGET_SAKSBEHANDLINGSTID_MEDL_FORUTGÅENDE -> DokumentMal.FORLENGET_SAKSBEHANDLINGSTID_MEDL_FORUTGÅENDE;
            case FORLENGET_SAKSBEHANDLINGSTID_TIDLIG -> DokumentMal.FORLENGET_SAKSBEHANDLINGSTID_TIDLIG;
            case KLAGE_AVVIST -> DokumentMal.KLAGE_AVVIST;
            case KLAGE_OMGJORT -> DokumentMal.KLAGE_OMGJORT;
            case KLAGE_OVERSENDT -> DokumentMal.KLAGE_OVERSENDT;
            case ETTERLYS_INNTEKTSMELDING -> DokumentMal.ETTERLYS_INNTEKTSMELDING;
            case ENDRING_UTBETALING -> DokumentMal.ENDRING_UTBETALING;
            case FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_INFOBREV -> DokumentMal.FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_INFOBREV;
            case FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_FORLENGET_SAKSBEHANDLINGSTID -> DokumentMal.FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_FORLENGET_SAKSBEHANDLINGSTID;
            case KLAGE_AVVIST_DOK,
                 KLAGE_AVVIST_FRITEKST,
                 KLAGE_HJEMSENDT_DOK,
                 KLAGE_HJEMSENDT_FRITEKST,
                 KLAGE_OMGJORT_DOK,
                 KLAGE_OMGJORT_FRITEKST,
                 KLAGE_OVERSENDT_DOK,
                 KLAGE_OVERSENDT_FRITEKST,
                 KLAGE_STADFESTET_DOK,
                 KLAGE_STADFESTET_FRITEKST,
                 ANKE_OMGJORT_FRITEKST,
                 ANKE_OPPHEVET_FRITEKST,
                 ANKE_OMGJORT,
                 ANKE_OPPHEVET,
                 KLAGE_STADFESTET,
                 KLAGE_HJEMSENDT -> null;
            case null -> null;
        };
    }

    public static RevurderingÅrsak mapRevurderignÅrsak(RevurderingVarslingÅrsak revurderingVarslingÅrsak) {
        return switch (revurderingVarslingÅrsak) {
            case MOR_AKTIVITET_IKKE_OPPFYLT -> RevurderingÅrsak.MOR_AKTIVITET_IKKE_OPPFYLT;
            case ARBEID_I_UTLANDET -> RevurderingÅrsak.ARBEID_I_UTLANDET;
            case IKKE_LOVLIG_OPPHOLD -> RevurderingÅrsak.IKKE_LOVLIG_OPPHOLD;
            case OPPTJENING_IKKE_OPPFYLT -> RevurderingÅrsak.OPPTJENING_IKKE_OPPFYLT;
            case ARBEIDS_I_STØNADSPERIODEN -> RevurderingÅrsak.ARBEIDS_I_STØNADSPERIODEN;
            case BEREGNINGSGRUNNLAG_UNDER_HALV_G -> RevurderingÅrsak.BEREGNINGSGRUNNLAG_UNDER_HALV_G;
            case BRUKER_REGISTRERT_UTVANDRET -> RevurderingÅrsak.BRUKER_REGISTRERT_UTVANDRET;
            case BARN_IKKE_REGISTRERT_FOLKEREGISTER -> RevurderingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;
            case ANNET -> RevurderingÅrsak.ANNET;
            case null -> null;
        };
    }
}
