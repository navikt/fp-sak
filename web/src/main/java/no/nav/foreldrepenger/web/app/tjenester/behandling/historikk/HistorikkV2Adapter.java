package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_FOM;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_TOM;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

public class HistorikkV2Adapter {

    private static final Logger LOG = LoggerFactory.getLogger(HistorikkV2Adapter.class);

    public static HistorikkinnslagDtoV2 map(Historikkinnslag h, UUID behandlingUUID, List<JournalpostId> journalPosterForSak, URI dokumentPath) {
        return switch (h.getType()) {
            case BEH_GJEN, KØET_BEH_GJEN, BEH_MAN_GJEN, BEH_STARTET, BEH_STARTET_PÅ_NYTT,
                 // BEH_STARTET_FORFRA,  finnes ikke lenger? fptilbake?
                 VEDLEGG_MOTTATT, BREV_SENT, BREV_BESTILT, MIN_SIDE_ARBEIDSGIVER, REVURD_OPPR, REGISTRER_PAPIRSØK, MANGELFULL_SØKNAD, INNSYN_OPPR,
                 VRS_REV_IKKE_SNDT, NYE_REGOPPLYSNINGER, BEH_AVBRUTT_VUR, BEH_OPPDATERT_NYE_OPPL, SPOLT_TILBAKE,
                 // TILBAKEKREVING_OPPR,  fptilbake
                 MIGRERT_FRA_INFOTRYGD, MIGRERT_FRA_INFOTRYGD_FJERNET, ANKEBEH_STARTET, KLAGEBEH_STARTET, ENDRET_DEKNINGSGRAD, OPPGAVE_VEDTAK ->
                fraMaltype1(h, behandlingUUID, journalPosterForSak, dokumentPath);
            case FORSLAG_VEDTAK, FORSLAG_VEDTAK_UTEN_TOTRINN, VEDTAK_FATTET,
                 // VEDTAK_FATTET_AUTOMATISK, que? fptilbake?
                 UENDRET_UTFALL, REGISTRER_OM_VERGE -> fraMaltype2(h, behandlingUUID);
            case SAK_RETUR -> fraMaltype3(h, behandlingUUID);
            case AVBRUTT_BEH,
                BEH_KØET,
                BEH_VENT,
                IVERKSETTELSE_VENT,
                FJERNET_VERGE -> fraMalType4(h, behandlingUUID);
            case SAK_GODKJENT, FAKTA_ENDRET, KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, UTTAK, TERMINBEKREFTELSE_UGYLDIG, ANKE_BEH ->
                fraMalType5(h, behandlingUUID, journalPosterForSak, dokumentPath);
            case NY_INFO_FRA_TPS
                 //NY_GRUNNLAG_MOTTATT fptilbake? ja
                -> fraMalType6(h, behandlingUUID);
            case OVERSTYRT-> fraMalType7(h, behandlingUUID);
            case OPPTJENING -> throw new IllegalStateException(String.format("Kode: %s har ingen maltype", h.getType()));
            case OVST_UTTAK_SPLITT,
                FASTSATT_UTTAK_SPLITT
               // TILBAKEKR_VIDEREBEHANDLING fptilbake?
                -> fraMalType9(h, behandlingUUID);
            case OVST_UTTAK, FASTSATT_UTTAK -> fraMaltype10(h, behandlingUUID);
            case AVKLART_AKTIVITETSKRAV ->
                fraMalTypeAktivitetskrav(h, behandlingUUID);

            /* fptilbake
            case FAKTA_OM_FEILUTBETALING ->
                fraMaltypeFeilutbetaling(h, behandlingUUID);
            case FORELDELSE ->
                fraMaltypeForeldelse(h, behandlingUUID);
            case TILBAKEKREVING ->
                fraMaltypeTilbakekreving(h, behandlingUUID);
             */

            default -> null; //TODO fjerne default
        };
    }
    private static HistorikkinnslagDtoV2 fraMaltype1(Historikkinnslag innslag,
                                                     UUID behandlingUUID,
                                                     List<JournalpostId> journalPosterForSak,
                                                     URI dokumentPath) {
        var historikkinnslagDel = innslag.getHistorikkinnslagDeler().getFirst();
        var skjermlenke = historikkinnslagDel.getSkjermlenke().map(SkjermlenkeType::fraKode).orElse(null);
        var lenker = tilDto(innslag.getDokumentLinker(), journalPosterForSak, dokumentPath);
        var begrunnelsetekst = begrunnelseFraDel(historikkinnslagDel).map(List::of);

        var body = begrunnelsetekst.orElse(List.of());
        return new HistorikkinnslagDtoV2(behandlingUUID, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(innslag.getAktør(), innslag.getOpprettetAv()),
            skjermlenke, innslag.getOpprettetTidspunkt(), lenker, innslag.getType().getNavn(), body);
    }

    private static HistorikkinnslagDtoV2 fraMaltype2(Historikkinnslag h, UUID behandlingUUID) {
        var del = h.getHistorikkinnslagDeler().getFirst();
        var skjermlenke = del.getSkjermlenke().orElse(null); // TODO Thao: Sjekk om det alltid finnes en skjermlenke for type2?
        var skjermlenkeType = SkjermlenkeType.fraKode(skjermlenke);
        var tittel = skjermlenke != null ? skjermlenkeType.getNavn() : "";
        var hendelse = del.getHendelse();
        var resultat = del.getResultat();
        var tekst = "";

        if(resultat.isPresent() && hendelse.isPresent()){
            // TODO Thao: Er det greit å returnere tom string? Eller skal det kastes en exception her?
            var hendelseTekst = del.getHendelse().isPresent() ? HistorikkinnslagType.fraKode(del.getHendelse().get().getNavn()).getNavn() : "";

            // TODO Thao: Er det greit å returnere tom string? Eller skal det kastes en exception her?
            var resultatTekst = del.getResultat().isPresent() ? fraHistorikkResultat(del.getResultat().get()): "";

            tekst = String.format("%s : %s", hendelseTekst, resultatTekst);
        }

        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenkeType,
            h.getOpprettetTidspunkt(),
            null, // TODO
            tittel,
            Collections.singletonList(tekst));
    }

    private static HistorikkinnslagDtoV2 fraMaltype3(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
        var tittel = "TODO";
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var hendelseTekst = del.getHendelse().stream()
                .map(HistorikkV2Adapter::fraHendelseFelt)
                .toList();
            var aksjonspunkt = del.getTotrinnsvurderinger().stream()
                .map(HistorikkV2Adapter::fraAksjonspunktFelt)
                .toList();

            tekster.addAll(hendelseTekst);
            tekster.addAll(aksjonspunkt);
        }

        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenke.orElse(null),
            h.getOpprettetTidspunkt(),
            null, // TODO
            tittel,
            tekster);
    }

    private static String fraAksjonspunktFelt(HistorikkinnslagTotrinnsvurdering aksjonspunktFelt) {
        var aksjonspunktTekst = switch (aksjonspunktFelt.getAksjonspunktDefinisjon()) {
            case AVKLAR_TERMINBEKREFTELSE -> "Opplysninger om termin oppgitt i søknaden";
            case AVKLAR_ADOPSJONSDOKUMENTAJON -> "Adopsjonsopplysninger fra søknad";
            case AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN -> "Ektefelles/samboers barn";
            case AVKLAR_OM_SØKER_ER_MANN_SOM_ADOPTERER_ALENE -> "Mann adopterer";
            case MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET -> "Søknadsfrist";
            case MANUELL_VURDERING_AV_SØKNADSFRIST -> "Søknadsfrist";
            case AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE -> "Fakta om omsorg og foreldreansvar";
            case VURDER_PERIODER_MED_OPPTJENING -> "Opptjening";
            //case MANUELL_VURDERING_AV_OMSORGSVILKÅRET -> 'HistorikkEndretFeltVerdiType.ApplicationInformation'; TODO: Finner ikke tekststreng i frontend
            case REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD -> "Registrering av papirsøknad";
            case MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD -> "Foreldreansvaret";
            case MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD -> "Foreldreansvaret";
            // case UTGÅTT_5025 -> 'VarselOmRevurderingInfoPanel.Etterkontroll'; TODO: Finner ikke tekststreng i frontend
            //case VARSEL_REVURDERING_MANUELL -> 'VarselOmRevurderingInfoPanel.Manuell';  TODO: Finner ikke tekststreng i frontend
            case AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE -> "Vurder om engangsstønad eller foreldrepenger utbetalt til søker gjelder samme barn";
            case AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE -> "Vurder om engangsstønad eller foreldrepenger utbetalt til søker gjelder samme barn";
            case AVKLAR_VERGE -> "Avklar verge";
            case SJEKK_MANGLENDE_FØDSEL -> "Kontroller manglende opplysninger om fødsel";
            case MANUELL_VURDERING_AV_KLAGE_NFP -> "Fastsett resultatet av klagebehandlingen";
            case VURDERING_AV_FORMKRAV_KLAGE_NFP -> "Vurder om klagen oppfyller formkravene";
            case UTGÅTT_5080 -> "Avklar arbeidsforhold";
            case UTGÅTT_5019 -> "Avklar lovlig opphold";
            case UTGÅTT_5020 -> "Fastsett om søker er bosatt";
            case UTGÅTT_5023 -> "Avklar oppholdsrett";
            case OVERSTYRING_AV_FØDSELSVILKÅRET -> "Overstyring av fødselsvilkåret";
            case OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR -> "Overstyring av fødselsvilkåret";
            case OVERSTYRING_AV_ADOPSJONSVILKÅRET -> "Overstyring av adopsjonsvilkåret";
            //case OVERSTYRING_AV_ADOPSJONSVILKÅRET_FP -> 'Overstyr.adopsjonsvilkar'; TODO: Finner ikke tekststreng i frontend
            case OVERSTYRING_AV_OPPTJENINGSVILKÅRET -> "Overstyring av opptjeningsvilkåret";
            case OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET -> "Overstyring av medlemskapsvilkåret";
            case OVERSTYRING_AV_SØKNADSFRISTVILKÅRET -> "Overstyring av søknadsfristvilkåret";
            case OVERSTYRING_AV_BEREGNING -> "Overstyring av beregning";
            case OVERSTYRING_AV_UTTAKPERIODER -> "Overstyrte uttaksperioder";
            case MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG -> "Manuell kontroll av om søker har aleneomsorg";
            case AVKLAR_LØPENDE_OMSORG -> "Manuell kontroll av om søker har omsorg";
            case FASTSETT_UTTAKPERIODER -> "Manuelt fastsatte uttaksperioder";
            case KONTROLLER_OPPLYSNINGER_OM_DØD -> "Kontroller opplysninger om død";
            case KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST -> "Kontroller opplysninger om søknadsfrist";
            case KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE -> "Kontroller opplysninger om realitetsbehandling/klage";
            case KONTROLLER_ANNENPART_EØS -> "Kontroller opplysninger om annen forelders uttak i EØS";
            case UTGÅTT_5067 -> "Kontroller evt overlappende uttak mot brukers senere saker";
            case UTGÅTT_5075 -> "Kontroller opplysninger om fordeling av stønadsperioden";
            case FASTSETT_UTTAK_STORTINGSREPRESENTANT -> "Søker er stortingsrepresentant. Uttak";
            case FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS -> "Fastsatt beregningsgrunnlag for arbeidstaker/frilanser";
            case FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD -> "Fastsatt beregningsgrunnlag for kortvarig arbeidsforhold";
            case VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE -> "Vurdering av varig endret eller nyoppstartet næring";
            case FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET -> "Fastsatt beregningsgrunnlag for selvstendig næring ny i arbeidslivet";
            case VURDER_FAKTA_FOR_ATFL_SN -> "Vurder fakta for beregning";
            case FORESLÅ_VEDTAK -> "Fritekstbrev";
            case AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT -> "Vurdering om den andre forelderen har rett til foreldrepenger";
            //default -> throw new IllegalStateException("Unexpected value: " + aksjonspunktFelt.getAksjonspunktDefinisjon()); // TODO
            default -> null;
        };
            // TODO sjekk denne case
            /*case UTGÅTT_5078 -> "Kontroller opplysninger om tilstøtende ytelser innvilget";
            case UTGÅTT_5079 -> "Kontroller opplysninger om tilstøtende ytelser opphørt";*/
            // FASTSETT_BRUTTO_BEREGNINGSGRUNNLAG_SELVSTENDIG_NAERINGSDRIVENDE -> 'Historikk.BeregningsgrunnlagManueltSN';

        String tekst;
        if (aksjonspunktFelt.erGodkjent()) {
            tekst = aksjonspunktTekst  != null ? aksjonspunktTekst + " er godkjent" : "Er godkjent";
        } else {
            var ikkeGodkjentTekst = aksjonspunktTekst != null ? aksjonspunktTekst + " må vurderes på nytt" : "Må vurderes på nytt";
            var begrunnelse = aksjonspunktFelt.getBegrunnelse();
            tekst = (begrunnelse != null && !begrunnelse.isEmpty()) ? ikkeGodkjentTekst + "\n" + begrunnelse : ikkeGodkjentTekst;
        }

        return tekst;
    }

    private static HistorikkinnslagDtoV2 fraMalType4(Historikkinnslag h, UUID behandlingUUID) {
        return null;
    }

    private static HistorikkinnslagDtoV2 fraMalType5(Historikkinnslag h, UUID behandlingUUID,
                                                     List<JournalpostId> journalPosterForSak,
                                                     URI dokumentPath) {
        var skjermlenke = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
        var lenker = tilDto(h.getDokumentLinker(), journalPosterForSak, dokumentPath);
        var tittel = switch (h.getType()) {
            case KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, ANKE_BEH -> h.getType().getNavn();
            default -> null;
        };
        return new HistorikkinnslagDtoV2(behandlingUUID, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenke.orElse(null), h.getOpprettetTidspunkt(), lenker, tittel, lagTekstForMal5(h));
    }

    private static HistorikkinnslagDtoV2 fraMalType6(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<String>();
        for (var del : h.getHistorikkinnslagDeler()) {
            var hendelseTekst = del.getHendelse().stream()
                .map(HistorikkV2Adapter::fraHendelseFelt)
                .toList();
            var opplysninger = del.getOpplysninger().stream()
                .map(HistorikkV2Adapter::fraOpplysning)
                .toList();

            tekster.addAll(hendelseTekst);
            tekster.addAll(opplysninger);
        }
        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            null,
            h.getOpprettetTidspunkt(),
            null,
            null,
            tekster);

    }

    private static HistorikkinnslagDtoV2 fraMalType7(Historikkinnslag h, UUID behandlingUUID) {
        return null;
    }

    private static HistorikkinnslagDtoV2 fraMalType9(Historikkinnslag h, UUID behandlingUUID) {
        return null;
    }

    private static HistorikkinnslagDtoV2 fraMaltype10(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
        var tittel = "TODO";
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var periodeFom = del.getOpplysninger().stream()
                .filter(o -> UTTAK_PERIODE_FOM.getKode().equals(o.getNavn()))
                .map(HistorikkinnslagFelt::getTilVerdi)
                .findFirst()
                .orElse("");

            var periodeTom = del.getOpplysninger().stream()
                .filter(o -> UTTAK_PERIODE_TOM.getKode().equals(o.getNavn()))
                .map(HistorikkinnslagFelt::getTilVerdi)
                .findFirst()
                .orElse("");

            var opplysningTekst = "";
            if (h.getType().equals(HistorikkinnslagType.OVST_UTTAK)) {
                opplysningTekst = String.format("<b>Overstyrt vurdering</b> av perioden %s - %s.", periodeFom, periodeTom);

            }
            if (h.getType().equals(HistorikkinnslagType.FASTSATT_UTTAK)) {
                opplysningTekst = String.format("<b>Manuell vurdering</b> av perioden %s - %s.", periodeFom, periodeTom);
            }

            var endretFelter = del.getEndredeFelt().stream()
                .map(HistorikkV2Adapter::fraEndretFeltMalType10)
                .toList();

            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            tekster.add(opplysningTekst);
            tekster.addAll(endretFelter);
            tekster.addAll(begrunnelsetekst);
        }

        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenke.orElse(null),
            h.getOpprettetTidspunkt(),
            null, // TODO
            tittel,
            tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalTypeAktivitetskrav(Historikkinnslag h, UUID behandlingUUID) {
        return null;
    }

    private static String fraHendelseFelt(HistorikkinnslagFelt felt) {
        return HistorikkinnslagType.fraKode(felt.getNavn()).getNavn();
    }

    private static String fraEndretFeltMalType10(HistorikkinnslagFelt felt) {
        var tekst = "";
        var fieldName = kodeverdiTilStreng(HistorikkEndretFeltType.fraKode(felt.getNavn()), felt.getNavnVerdi());

        if (HistorikkEndretFeltType.UTTAK_TREKKDAGER.getKode().equals(felt.getNavn())) {
            var fraVerdi = Double.parseDouble(felt.getFraVerdi());
            var tilVerdi = Double.parseDouble(felt.getTilVerdi());
            var fraVerdiUker = (int) Math.floor(fraVerdi / 5);
            var fraVerdiDager = fraVerdi % 1 == 0 ? fraVerdi % 5 : (Math.round(fraVerdi % 5 * 10) / 10.0);
            var tilVerdiUker = (int) Math.floor(tilVerdi / 5);
            var tilVerdiDager = tilVerdi % 1 == 0 ? tilVerdi % 5 : (Math.round(tilVerdi % 5 * 10) / 10.0); //TODO test gradering der trekkdager er feks 10,5, burde oversttes til 2 uker og 0,5 dager

            tekst = String.format("<b>%s</b> er endret fra %s uker og %s dager til <b>%s uker og %s dager</b>", fieldName, fraVerdiUker,
                fraVerdiDager, tilVerdiUker, tilVerdiDager);
        } else {
            tekst = historikkFraTilVerdi(felt, fieldName);
        }

        return tekst;
    }

    private static String historikkFraTilVerdi(HistorikkinnslagFelt felt, String fieldName) {
        var fraVerdi = finnEndretFeltVerdi(felt, felt.getFraVerdi());
        var tilVerdi = finnEndretFeltVerdi(felt, felt.getTilVerdi());
        var tekstMeldingTil = String.format("<b>%s</b> er satt til <b>%s</b>", fieldName, tilVerdi);
        var tekstMeldingEndretFraTil = String.format("<b>%s</b> er endret fra %s til <b>%s</b>", fieldName, fraVerdi, tilVerdi);
        var tekst = fraVerdi != null ? tekstMeldingEndretFraTil : tekstMeldingTil;

        if (HistorikkEndretFeltType.UTTAK_PROSENT_UTBETALING.getKode().equals(felt.getNavn()) && fraVerdi != null) {
            tekst = String.format("<b>%s</b> er endret fra %s %% til <b>%s %%</b>", fieldName, fraVerdi, tilVerdi);
        } else if (HistorikkEndretFeltType.UTTAK_PROSENT_UTBETALING.getKode().equals(felt.getNavn())) {
            tekst = String.format("<b>%s</b> er satt til <b>%s%%</b>", fieldName, tilVerdi);
        } else if (HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_TYPE.getKode().equals(felt.getNavn()) && "MANUELL_BEHANDLING".equals(felt.getFraVerdi())) {
            tekst = tekstMeldingTil;
        } else if (HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_ÅRSAK.getKode().equals(felt.getNavn())
            || HistorikkEndretFeltType.UTTAK_GRADERING_AVSLAG_ÅRSAK.getKode().equals(felt.getNavn())) {

            if ("_".equals(felt.getTilVerdi())) {
                return "";
            }
            if ("_".equals(felt.getFraVerdi())) {
                tekst = tekstMeldingTil;
            }
        } else if (HistorikkEndretFeltType.UTTAK_STØNADSKONTOTYPE.getKode().equals(felt.getNavn()) && "_".equals(felt.getFraVerdi())) {
            tekst = tekstMeldingTil;
        }

        return tekst;
    }

    private static List<String> lagTekstForMal5(Historikkinnslag h) {
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var hendelseTekst = del.getHendelse().stream()
                .map(HistorikkV2Adapter::fraHendelseFelt)
                .toList();
            var resultatTekst = del.getResultat().stream()
                .map(HistorikkV2Adapter::fraHistorikkResultat)
                .toList();
            var gjeldendeFraInnslag = del.getGjeldendeFraFelt().stream()
                .map(felt -> tilGjeldendeFraInnslag(felt, del))
                .toList();
            var søknadsperiode = del.getAvklartSoeknadsperiode().stream()
                .map(HistorikkV2Adapter::fraSøknadsperiode)
                .toList();
            var tema = del.getTema().stream()
                .map(HistorikkV2Adapter::fraTema)
                .toList();
            var endretFelter = del.getEndredeFelt().stream()
                .map(HistorikkV2Adapter::fraEndretFelt)
                .toList();
            var opplysninger = del.getOpplysninger().stream()
                .map(HistorikkV2Adapter::fraOpplysning)
                .toList();
            var årsaktekst = del.getAarsakFelt().stream()
                .flatMap(felt -> finnÅrsakKodeListe(felt).stream())
                .map(Kodeverdi::getNavn)
                .toList();
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            tekster.addAll(hendelseTekst);
            tekster.addAll(resultatTekst);
            tekster.addAll(gjeldendeFraInnslag);
            tekster.addAll(søknadsperiode);
            tekster.addAll(tema);
            tekster.addAll(endretFelter);
            tekster.addAll(opplysninger);
            tekster.addAll(årsaktekst);
            tekster.addAll(begrunnelsetekst);
        }
        return tekster.stream().toList();
    }

    private static String fraOpplysning(HistorikkinnslagFelt opplysning) {
        var historikkOpplysningType = HistorikkOpplysningType.fraKode(opplysning.getNavn());

        return switch (historikkOpplysningType) {
            case ANTALL_BARN -> "<b>Antall barn</b> som brukes i behandlingen: <b>{antallBarn}</b>".replace("{antallBarn}", opplysning.getTilVerdi()); // Brukes bare av maltype 5
            case TPS_ANTALL_BARN -> "Antall barn {verdi}".replace("{verdi}", opplysning.getTilVerdi());  // Brukes av maltype 6
            case FODSELSDATO -> "Når ble barnet født? {verdi}".replace("{verdi}", opplysning.getTilVerdi()); // Brukes av maltype 6
            //case UTTAK_PERIODE_FOM -> historikkOpplysningType.getNavn(); // Brukes av maltype 10 + aktivitetskrav
            //case UTTAK_PERIODE_TOM -> historikkOpplysningType.getNavn(); // Brukes av maltype 10 + aktivitetskrav
            default -> throw new IllegalStateException("Unexpected value: " + historikkOpplysningType);
        };
    }

    private static String finnEndretFeltVerdi(HistorikkinnslagFelt felt, Object verdi) {
        if (verdi == null) {
            return null;
        }
        if (isBoolean(String.valueOf(verdi))) {
            return konverterBoolean(String.valueOf(verdi));
        }
        if (felt.getKlTilVerdi() != null) {
            try {
                return kodeverdiTilStrengEndretFeltTilverdi(String.valueOf(verdi), String.valueOf(verdi));
            } catch (IllegalStateException e) {
                return String.format("EndretFeltTypeTilVerdiKode %s finnes ikke-LEGG DET INN", felt.getTilVerdiKode());
            }
        }
        return String.valueOf(verdi);
    }

    private static boolean isBoolean(String str) {
        return "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str);
    }

    // TODO Thao: Gjør om denne til felles klasse som kan brukes av flere
    //  type. Flytt denne til en util klasse
    private static String fraEndretFelt(HistorikkinnslagFelt felt) {
        var endretFeltNavn = HistorikkEndretFeltType.fraKode(felt.getNavn());

        var feltNavn = kodeverdiTilStreng(endretFeltNavn, felt.getNavnVerdi());

        var tilVerdi = konverterBoolean(felt.getTilVerdi());
        if (felt.getTilVerdi() != null && tilVerdi == null) {
            tilVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getTilVerdiKode(), felt.getTilVerdi());
        }

        if (felt.getFraVerdi() == null || endretFeltNavn.equals(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL)) {
            return String.format("<b>%s</b> er satt til <b>%s</b>.", feltNavn, tilVerdi);
        }

        var fraVerdi = konverterBoolean(felt.getFraVerdi());
        if (fraVerdi == null) {
            fraVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getFraVerdiKode(), felt.getFraVerdi());
        }

        return String.format("<b>%s</b> endret fra %s til <b>%s</b>", feltNavn, fraVerdi, tilVerdi);

    }

    private static String konverterBoolean(String verdi) {
        if ("true".equalsIgnoreCase(verdi)) {
            return "Ja";
        }
        if ("false".equalsIgnoreCase(verdi)) {
            return "Nei";
        }
        return null;
    }

    private static String kodeverdiTilStrengEndretFeltTilverdi(String verdiKode, String verdi) {
        if (verdiKode == null) {
            return verdi;
        }

        return FeltType.getByKey(verdiKode).getText();
    }

    private static String kodeverdiTilStreng(HistorikkEndretFeltType endretFeltNavn, String verdi) {
        var tekstFrontend = FeltNavnType.getByKey(endretFeltNavn.getKode()).getText();

        if (tekstFrontend.contains("{value}")) {
            if (verdi == null) {
                LOG.info("historikkv2 manglende value - {} {}", endretFeltNavn, tekstFrontend);
            } else {
                return tekstFrontend.replace("{value}", verdi);
            }
        }
        return tekstFrontend;
    }

    private static String fraTema(HistorikkinnslagFelt tema) {
        var type = HistorikkEndretFeltType.fraKode(tema.getNavn());
        var tekst = switch (type) {
            case AKTIVITET -> "<b>Det er lagt til ny aktivitet:</b> <br/><b>{value}</b>"; // Finnes ikke frontend
            case FORDELING_FOR_NY_ANDEL -> "<b>Det er lagt til ny aktivitet:</b> <br/><b>{value}</b>: Fordeling for ";
            case FORDELING_FOR_ANDEL -> "Fordeling for <b>{value}</b>:";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        return String.format(tekst, tema.getNavnVerdi());
    }

    private static String fraSøknadsperiode(HistorikkinnslagFelt søknadsperiode) {
        var type = HistorikkAvklartSoeknadsperiodeType.fraKode(søknadsperiode.getNavn());

        var tekst = switch (type) {
            case GRADERING -> "<b>Uttak: gradering</b> <br/>%s<br/>%s";
            case UTSETTELSE_ARBEID -> "<b>Utsettelse: Arbeid</b> <br/>%s";
            case UTSETTELSE_FERIE -> "<b>Utsettelse: Ferie</b> <br/>%s";
            case UTSETTELSE_SKYDOM -> "<b>Utsettelse: Sykdom/skade</b> <br/>%s";
            case UTSETTELSE_HV -> "<b>Utsettelse: Heimevernet</b> <br/>%s";
            case UTSETTELSE_TILTAK_I_REGI_AV_NAV -> "<b>Utsettelse: Tiltak i regi av NAV</b> <br/>%s";
            case UTSETTELSE_INSTITUSJON_SØKER -> "<b>Utsettelse: Innleggelse av forelder</b> <br/>%s";
            case UTSETTELSE_INSTITUSJON_BARN -> "<b>Utsettelse: Innleggelse av barn</b> <br/>%s";
            case NY_SOEKNADSPERIODE -> "<b>Ny periode er lagt til</b> <br/>%s";
            case SLETTET_SOEKNASPERIODE -> "<b>Perioden er slettet</b> <br/>%s";
            case OVERFOERING_ALENEOMSORG -> "<b>Overføring: søker har aleneomsorg</b> <br/>%s";
            case OVERFOERING_SKYDOM -> "<b>Overføring: sykdom/skade</b> <br/>%s";
            case OVERFOERING_INNLEGGELSE -> "<b>Overføring: innleggelse</b> <br/>%s";
            case OVERFOERING_IKKE_RETT -> "<b>Overføring: annen forelder har ikke rett</b> <br/>%s";
            case UTTAK -> "<b>Uttak</b> <br/>%s";
            case OPPHOLD -> "<b>Opphold: annen foreldres uttak</b> <br/>%s";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        return type.equals(HistorikkAvklartSoeknadsperiodeType.GRADERING) ? String.format(tekst, søknadsperiode.getNavnVerdi(),
            søknadsperiode.getTilVerdi()) : String.format(tekst, søknadsperiode.getTilVerdi());
    }

    private static String tilGjeldendeFraInnslag(HistorikkinnslagFelt gjeldendeFra, HistorikkinnslagDel del) {
        // Fra DB: Hverken navn, navnverdi, tilVerdi er null (FPSAK)
        var historikkEndretFeltType = HistorikkEndretFeltType.fraKode(gjeldendeFra.getNavn());

        var endretFeltTekst = switch (historikkEndretFeltType) {
            case NY_AKTIVITET -> "Det er lagt til ny aktivitet for <b>%s</b>";
            case NY_FORDELING -> "Ny fordeling <b>%s</b>";
            default -> throw new IllegalArgumentException();
        };
        // Historikk.Template.5.VerdiGjeldendeFra
        var verditekst = String.format(" gjeldende fra <b>%s</b>:", gjeldendeFra.getTilVerdi());

        return String.format(endretFeltTekst, gjeldendeFra.getNavnVerdi()) + verditekst + (del.getEndredeFelt()
            .isEmpty() ? "Ingen endring av vurdering" : "");
    }


    private static String fraHistorikkResultat(String type) {
        var historikkResultatType = HistorikkResultatType.valueOf(type);
        return switch (historikkResultatType) {
            case MEDHOLD_I_KLAGE -> "Vedtaket er omgjort";
            case OPPHEVE_VEDTAK -> "Vedtaket er opphevet";
            case OPPRETTHOLDT_VEDTAK -> "Vedtaket er opprettholdt";
            case STADFESTET_VEDTAK -> "Vedtaket er stadfestet";
            case BEREGNET_AARSINNTEKT -> "Grunnlag for beregnet årsinntekt";
            case UTFALL_UENDRET -> "Overstyrt vurdering: Utfall er uendret";
            case DELVIS_MEDHOLD_I_KLAGE -> "Vedtaket er delvis omgjort";
            case KLAGE_HJEMSENDE_UTEN_OPPHEVE -> "Behandling er hjemsendt";
            case UGUNST_MEDHOLD_I_KLAGE -> "Vedtaket er omgjort til ugunst";
            case OVERSTYRING_FAKTA_UTTAK -> "Overstyrt vurdering:";
            case ANKE_AVVIS, ANKE_OMGJOER, ANKE_OPPHEVE_OG_HJEMSENDE, ANKE_HJEMSENDE, ANKE_STADFESTET_VEDTAK, ANKE_DELVIS_OMGJOERING_TIL_GUNST,
                 ANKE_TIL_UGUNST, ANKE_TIL_GUNST -> historikkResultatType.getNavn(); // Ikke i frontend
            default -> historikkResultatType.getNavn();
        };
    }




    private static Optional<String> begrunnelseFraDel(HistorikkinnslagDel historikkinnslagDel) {
        return historikkinnslagDel.getBegrunnelseFelt()
            .flatMap(HistorikkV2Adapter::finnÅrsakKodeListe)
            .map(Kodeverdi::getNavn)
            .or(historikkinnslagDel::getBegrunnelse);
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDto(List<HistorikkinnslagDokumentLink> dokumentLinker,
                                                                List<JournalpostId> journalPosterForSak,
                                                                URI dokumentPath) {
        if (dokumentLinker == null) {
            return List.of();
        }
        return dokumentLinker.stream().map(d -> tilDto(d, journalPosterForSak, dokumentPath)) //
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDto(HistorikkinnslagDokumentLink lenke,
                                                          List<JournalpostId> journalPosterForSak,
                                                          URI dokumentPath) {
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        dto.setUtgått(aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak));
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        if (lenke.getJournalpostId().getVerdi() != null && lenke.getDokumentId() != null && dokumentPath != null) {
            var builder = UriBuilder.fromUri(dokumentPath)
                .queryParam("journalpostId", lenke.getJournalpostId().getVerdi())
                .queryParam("dokumentId", lenke.getDokumentId());
            dto.setUrl(builder.build());
        }
        return dto;
    }

    private static boolean aktivJournalPost(JournalpostId journalpostId, List<JournalpostId> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp, journalpostId)).findFirst().isEmpty();
    }


    // Fra HistorikkinnslagDelTo
    private static Optional<Kodeverdi> finnÅrsakKodeListe(HistorikkinnslagFelt aarsak) {

        var aarsakVerdi = aarsak.getTilVerdi();
        if (Objects.equals("-", aarsakVerdi)) {
            return Optional.empty();
        }
        if (aarsak.getKlTilVerdi() == null) {
            return Optional.empty();
        }

        var kodeverdiMap = HistorikkInnslagTekstBuilder.KODEVERK_KODEVERDI_MAP.get(aarsak.getKlTilVerdi());
        if (kodeverdiMap == null) {
            throw new IllegalStateException("Har ikke støtte for HistorikkinnslagFelt#klTilVerdi=" + aarsak.getKlTilVerdi());
        }
        return Optional.ofNullable(kodeverdiMap.get(aarsakVerdi));
    }
}
