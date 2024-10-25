package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;
import java.util.ArrayList;
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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_FOM;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_TOM;

public class HistorikkV2Adapter {

    private static final Logger LOG = LoggerFactory.getLogger(HistorikkV2Adapter.class);

    public static HistorikkinnslagDtoV2 map(Historikkinnslag h, UUID behandlingUUID) {
        return switch (h.getType()) {
            case BEH_GJEN, KØET_BEH_GJEN, BEH_MAN_GJEN, BEH_STARTET, BEH_STARTET_PÅ_NYTT,
                 // BEH_STARTET_FORFRA,  finnes ikke lenger? fptilbike?
                 VEDLEGG_MOTTATT, BREV_SENT, BREV_BESTILT, MIN_SIDE_ARBEIDSGIVER, REVURD_OPPR, REGISTRER_PAPIRSØK, MANGELFULL_SØKNAD, INNSYN_OPPR,
                 VRS_REV_IKKE_SNDT, NYE_REGOPPLYSNINGER, BEH_AVBRUTT_VUR, BEH_OPPDATERT_NYE_OPPL, SPOLT_TILBAKE,
                 // TILBAKEKREVING_OPPR, fptilbake
                 MIGRERT_FRA_INFOTRYGD, MIGRERT_FRA_INFOTRYGD_FJERNET, ANKEBEH_STARTET, KLAGEBEH_STARTET, ENDRET_DEKNINGSGRAD, OPPGAVE_VEDTAK ->
                fraMaltype1(h, behandlingUUID);
            case FORSLAG_VEDTAK, FORSLAG_VEDTAK_UTEN_TOTRINN, VEDTAK_FATTET,
                 // VEDTAK_FATTET_AUTOMATISK, que?
                 UENDRET_UTFALL, REGISTRER_OM_VERGE -> fraMaltype2(h, behandlingUUID);
            case SAK_GODKJENT, FAKTA_ENDRET, KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, UTTAK, TERMINBEKREFTELSE_UGYLDIG, ANKE_BEH ->
                fraMalType5(h, behandlingUUID);
            case OVST_UTTAK, FASTSATT_UTTAK -> fraMaltype10(h, behandlingUUID);
            default -> null; //TODO fjerne default
        };
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
            String opplysningTekst;
            if (h.getType().equals(HistorikkinnslagType.OVST_UTTAK)) {
                var tekst = "<b>Overstyrt vurdering</b> av perioden {periodeFom} - {periodeTom}.";

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

                tekst.replace("{periodeFom}", periodeFom);
                tekst.replace("{periodeTom}", periodeTom);
                opplysningTekst = tekst;
            }
            if (h.getType().equals(HistorikkinnslagType.FASTSATT_UTTAK)) {
                // opplysing
                var tekst = "<b>Manuell vurdering</b> av perioden {periodeFom} - {periodeTom}.";
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
                tekst.replace("{periodeFom}", periodeFom);
                tekst.replace("{periodeTom}", periodeTom);
                opplysningTekst = tekst;
            }

            // Endret felt
            var tekst = "<b>{fieldName}</b> er endret fra {fromValueWeeks} uker og {fromValueDays} dager til <b>{toValueWeeks} uker og {toValueDays} dager</b>";
            var fieldName = kodeverdiTilStreng(HistorikkEndretFeltType.fraKode(felt.getNavn()), felt.getNavnVerdi());
            var fromValueWeeks = ;
            var fromValueDays = ;
            var toValueWeeks = ;
            var toValueDays = ;

            tekster.add();
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

    private static HistorikkinnslagDtoV2 fraMalType5(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
        var lenker = tilDto(h.getDokumentLinker());
        var tittel = switch (h.getType()) {
            case KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, ANKE_BEH -> h.getType().getNavn();
            default -> null;
        };
        return new HistorikkinnslagDtoV2(behandlingUUID, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenke.orElse(null), h.getOpprettetTidspunkt(), lenker, tittel, lagTekstForMal5(h));
    }

    /**
     * Slik er det nå (dette er feil):
     * Hendelse 1
     * Hendelse 2
     * Resultat 1
     * Resultat 2
     *
     * Dette er riktig:
     * Hendelse 1
     * Resultat 1
     * Hendelse 2
     * Resultat 2
     */
    private static List<String> lagTekstForMal5(Historikkinnslag h) {
        var hendelseTekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getHendelse().stream())
            .map(HistorikkinnslagFelt::getNavn)
            .map(HistorikkinnslagType::fraKode)
            .map(HistorikkinnslagType::getNavn)
            .toList();

        var resultatTekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getResultat().stream())
            .map(HistorikkV2Adapter::fraHistorikkResultat)
            .toList();

        var gjeldendeFraInnslag = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getGjeldendeFraFelt().stream().map(felt -> tilGjeldendeFraInnslag(felt, del)))
            .toList();

        var søknadsperiode = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getAvklartSoeknadsperiode().stream())
            .map(HistorikkV2Adapter::fraSøknadsperiode)
            .toList();

        var tema = h.getHistorikkinnslagDeler().stream().flatMap(del -> del.getTema().stream()).map(HistorikkV2Adapter::fraTema).toList();

        var endretFelter = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getEndredeFelt().stream())
            .map(HistorikkV2Adapter::fraEndretFelt)
            .toList();

        var opplysninger = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getOpplysninger().stream())
            .map(HistorikkV2Adapter::fraOpplysning)
            .toList();

        var årsaktekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getAarsakFelt().stream())
            .flatMap(felt -> finnÅrsakKodeListe(felt).stream())
            .map(Kodeverdi::getNavn)
            .toList();

        var begrunnelsetekst = h.getHistorikkinnslagDeler().stream().flatMap(d -> begrunnelseFraDel(d).stream()).toList();

        var body = new ArrayList<>(hendelseTekst);
        body.addAll(hendelseTekst);
        body.addAll(resultatTekst);
        body.addAll(gjeldendeFraInnslag);
        body.addAll(søknadsperiode);
        body.addAll(tema);
        body.addAll(endretFelter);
        body.addAll(opplysninger);
        body.addAll(årsaktekst);
        body.addAll(begrunnelsetekst);
        return body;
    }

    private static String fraOpplysning(HistorikkinnslagFelt opplysning) {
        var historikkOpplysningType = HistorikkOpplysningType.fraKode(opplysning.getNavn());
        var tekst = switch (historikkOpplysningType) {
            case ANTALL_BARN -> "<b>Antall barn</b> som brukes i behandlingen: <b>{antallBarn}</b>";
            case TPS_ANTALL_BARN -> "Antall barn";
            case FODSELSDATO -> "Når ble barnet født?";
            case UTTAK_PERIODE_FOM -> historikkOpplysningType.getNavn(); // TODO: ingen feltid frontend?
            case UTTAK_PERIODE_TOM -> historikkOpplysningType.getNavn(); // TODO: ingen feltid frontend?
            default -> throw new IllegalStateException("Unexpected value: " + historikkOpplysningType);
        };
        return tekst.replace("{antallBarn}", opplysning.getTilVerdi());
    }

    private static String fraEndretFelt(HistorikkinnslagFelt felt) {
        var endretendretFeltNavnFeltNavn = HistorikkEndretFeltType.fraKode(felt.getNavn());

        var feltNavn = kodeverdiTilStreng(endretendretFeltNavnFeltNavn, felt.getNavnVerdi());
        var tilVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getTilVerdiKode(), felt.getTilVerdiKode());

        if (felt.getFraVerdi() == null || endretendretFeltNavnFeltNavn.equals(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL)) {
            return String.format("<b>%s</b> er satt til <b>%s</b>.", feltNavn, tilVerdi);
        } else {
            var fraVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getFraVerdiKode(), felt.getFraVerdi());
            return String.format("<b>%s</b> endret fra %s til <b>%s</b>", feltNavn, fraVerdi, tilVerdi);
        }
    }

    private static String kodeverdiTilStrengEndretFeltTilverdi(String verdiKode, String verdi) {
        if (verdiKode == null) {
            return verdi;
        }

        return switch (verdiKode) { // TODO: Hent disse fra historikkEndretFeltVerdiTypeCodes.tsx som henter en tekst fra nb_NO.json
            case "INNVILGET" -> "";
            case "AVSLÅTT" -> "";
            case "2003" -> "";
            case "VILKAR_OPPFYLT" -> "";
            case "2011" -> "";
            case "ARBEIDSTAKER" -> "";
            case "I_AKTIVITET" -> "";
            case "2002" -> "";
            case "2031" -> "";
            case "FEDREKVOTE" -> "";
            case "FELLESPERIODE" -> "";
            case "2038" -> "";
            case "LOVLIG_OPPHOLD" -> "";
            case "MØDREKVOTE" -> "";
            case "4002" -> "";
            case "GRADERING_OPPFYLT" -> "";
            case "2004" -> "";
            case "2030" -> "";
            case "ANNEN_FORELDER_HAR_IKKE_RETT" -> "";
            case "TILBAKEKR_OPPRETT" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_AVKLARES_IKKE" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT" -> "";
            case "HAR_GYLDIG_GRUNN" -> "";
            case "BRUK_MED_OVERSTYRTE_PERIODER" -> "";
            case "GRADERING_IKKE_OPPFYLT" -> "";
            case "2010" -> "";
            case "IKKE_BENYTT" -> "";
            case "VARIG_ENDRET_NAERING" -> "";
            case "FORELDREPENGER" -> "";
            case "INGEN_INNVIRKNING" -> "";
            case "2037" -> "";
            case "VILKAR_IKKE_OPPFYLT" -> "";
            case "FORTSETT_UTEN_INNTEKTSMELDING" -> "";
            case "MANGLENDE_OPPLYSNINGER" -> "";
            case "IKKE_OPPFYLT" -> "";
            case "BENYTT" -> "";
            case "KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT" -> "";
            case "TILBAKEKR_IGNORER" -> "";
            case "SELVSTENDIG_NÆRINGSDRIVENDE" -> "";
            case "2016" -> "";
            case "2033" -> "";
            case "4084" -> "";
            case "INNTEKT_IKKE_MED_I_BG" -> "";
            case "2021" -> "";
            case "OPPFYLT" -> "oppfylt";
            case "SØKER_ER_IKKE_I_PERMISJON" -> "";
            case "BOSATT_I_NORGE" -> "";
            case "INNVIRKNING" -> "";
            case "OPPHOLDSRETT" -> "";
            case "2036" -> "";
            case "BOSATT_UTLAND" -> "";
            case "SØKER_ER_I_PERMISJON" -> "";
            case "IKKE_BOSATT_I_NORGE" -> "";
            case "FRILANSER" -> "";
            case "2006" -> "";
            case "ANNEN_FORELDER_HAR_RETT" -> "";
            case "4060" -> "";
            case "DAGPENGER" -> "";
            case "INGEN_VARIG_ENDRING_NAERING" -> "";
            case "IKKE_BRUK" -> "";
            case "IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD" -> "";
            case "4076" -> "";
            case "HINDRE_TILBAKETREKK" -> "";
            case "4034" -> "";
            case "FORELDREPENGER_FØR_FØDSEL" -> "";
            case "IKKE_I_AKTIVITET_DOKUMENTERT" -> "";
            case "IKKE_I_AKTIVITET_IKKE_DOKUMENTERT" -> "";
            case "BENYTT_A_INNTEKT_I_BG" -> "";
            case "4005" -> "";
            case "IKKE_NY_I_ARBEIDSLIVET" -> "";
            case "4050" -> "";
            case "4082" -> "";
            case "TIDSBEGRENSET_ARBEIDSFORHOLD" -> "";
            case "4035" -> "";
            case "4051" -> "";
            case "HAR_IKKE_GYLDIG_GRUNN" -> "";
            case "EØS_BOSATT_NORGE" -> "";
            case "4066" -> "";
            case "4067" -> "";
            case "FASTSETT_RESULTAT_GRADERING_AVKLARES" -> "";
            case "2020" -> "";
            case "2024" -> "";
            case "4040" -> "";
            case "NY_I_ARBEIDSLIVET" -> "";
            case "2013" -> "";
            case "2023" -> "";
            case "2007" -> "";
            case "4020" -> "";
            case "2014" -> "";
            case "4023" -> "";
            case "4086" -> "";
            case "FASTSETT_RESULTAT_ENDRE_SOEKNADSPERIODEN" -> "";
            case "UTFØR_TILBAKETREKK" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT_IKKE" -> "";
            case "NASJONAL" -> "";
            case "4053" -> "";
            case "2028" -> "";
            case "4012" -> "";
            case "4501" -> "";
            case "2015" -> "";
            case "IKKE_LOVLIG_OPPHOLD" -> "";
            case "2034" -> "";
            case "ARBEIDSAVKLARINGSPENGER" -> "";
            case "2005" -> "";
            case "4503" -> "";
            case "4037" -> "";
            case "4102" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT_IKKE" -> "";
            case "4030" -> "";
            case "4061" -> "";
            case "IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING" -> "";
            case "4069" -> "";
            case "4007" -> "";
            case "2035" -> "";
            case "IKKE_ALENEOMSORG" -> "";
            case "4077" -> "";
            case "4022" -> "";
            case "FORTSETT_BEHANDLING" -> "";
            case "4074" -> "";
            case "4062" -> "";
            case "JORDBRUKER" -> "";
            case "ALENEOMSORG" -> "";
            case "4025" -> "";
            case "IKKE_NYOPPSTARTET" -> "";
            case "4092" -> "";
            case "4063" -> "";
            case "4055" -> "";
            case "4072" -> "";
            case "4073" -> "";
            case "4038" -> "";
            case "BOSA" -> "";
            case "4085" -> "";
            case "4033" -> "";
            case "2032" -> "";
            case "4095" -> "";
            case "NYOPPSTARTET" -> "";
            case "4087" -> "";
            case "MEDLEM" -> "";
            case "2017" -> "";
            case "4104" -> "";
            case "LAGT_TIL_AV_SAKSBEHANDLER" -> "";
            case "ARBEIDSTAKER_UTEN_FERIEPENGER" -> "";
            case "4081" -> "";
            case "OPPRETT_BASERT_PÅ_INNTEKTSMELDING" -> "";
            case "2026" -> "";
            case "4031" -> "";
            case "4052" -> "";
            case "2019" -> "";
            case "4056" -> "";
            case "4112" -> "";
            case "SJØMANN" -> "";
            case "2039" -> "";
            case "4041" -> "";
            case "IKKE_OPPHOLDSRETT" -> "";
            case "2022" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT" -> "";
            case "4098" -> "";
            case "4065" -> "";
            case "2027" -> "";
            case "UTVA" -> "";
            case "4093" -> "";
            case "4068" -> "";
            case "MANUELT_OPPRETTET_AV_SAKSBEHANDLER" -> "";
            case "4502" -> "";
            case "2012" -> "";
            case "4003" -> "";
            case "4057" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT" -> "";
            case "4088" -> "";
            case "FISKER" -> "";
            case "4523" -> "";
            case "PRAKSIS_UTSETTELSE" -> "";
            case "4107" -> "";
            case "4504" -> "";
            case "GRADERING_PÅ_ANDEL_UTEN_BG_IKKE_SATT_PÅ_VENT" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT_IKKE" -> "";
            case "KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_ARBEIDSFORHOLD" -> "";
            case "4100" -> "";
            case "4075" -> "";
            case "IKKE_RELEVANT" -> "";
            case "4115" -> "";
            case "4103" -> "";
            case "4099" -> "";
            case "4089" -> "";
            case "4106" -> "";
            case "4117" -> "";
            case "4070" -> "";
            case "4032" -> "";
            case "4110" -> "";
            case "4096" -> "";
            case "FORELDREANSVAR_4_TITTEL" -> "";
            case "2018" -> "";
            case "4008" -> "";
            case "FJERN_FRA_BEHANDLINGEN" -> "";
            case "4054" -> "";
            case "4097" -> "";
            case "4064" -> "";
            case "DAGMAMMA" -> "";
            case "DOKUMENTERT" -> "";
            case "2025" -> "";
            case "4059" -> "";
            case "IKKE_EKTEFELLES_BARN" -> "";
            case "4105" -> "";
            case "HENLEGG_BEHANDLING" -> "";
            case "SAMMENSATT_KONTROLL" -> "";
            case "NYTT_ARBEIDSFORHOLD" -> "";
            case "4013" -> "";
            case "4071" -> "";
            case "ADOPTERER_IKKE_ALENE" -> "";
            case "IKKE_DOKUMENTERT" -> "";
            case "EKTEFELLES_BARN" -> "";
            case "4039" -> "";
            case "4111" -> "";
            case "FORELDREANSVAR_2_TITTEL" -> "";
            case "UNNTAK" -> "";
            case "4058" -> "";
            case "4116" -> "";
            case "OMSORGSVILKARET_TITTEL" -> "";
            case "ADOPTERER_ALENE" -> "";
            case "FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT_IKKE" -> "";
            case "VERGE" -> "";
            default -> throw new IllegalStateException("Unexpected value: " + felt);
        };
    }

    private static String kodeverdiTilStreng(HistorikkEndretFeltType endretFeltNavn, String verdi) {
        var tekstFrontend = switch (endretFeltNavn) {
            case ADOPSJONSVILKARET -> "Adopsjonsvilkåret";
            case OPPTJENINGSVILKARET -> "Opptjeningsvilkåret";
            case MEDLEMSKAPSVILKÅRET -> "Medlemskap";
            case MEDLEMSKAPSVILKÅRET_OPPHØRSDATO -> "Opphørt medlemskap";
            case MEDLEMSKAPSVILKÅRET_MEDLEMFRADATO -> "Innflyttingsdato";
            case ADOPTERER_ALENE -> "Adopterer alene";
            case AKTIVITET -> "Aktivitet {value}";
            case AKTIVITET_PERIODE -> "Perioden med aktivitet {value} er";
            case ALENEOMSORG -> "Aleneomsorg";
            case ANTALL_BARN -> "Antall barn";
            case AVKLARSAKSOPPLYSNINGER -> "Personstatus";
            case BEHANDLENDE_ENHET -> "Behandlende enhet";
            case BEHANDLING -> "Behandling";
            case BRUK_ANTALL_I_SOKNAD -> "Bruk antall fra søknad";
            case BRUK_ANTALL_I_TPS -> "Bruk antall fra folkeregisteret";
            case BRUK_ANTALL_I_VEDTAKET -> "Bruk antall fra vedtaket";
            case BRUTTO_NAERINGSINNTEKT -> "Brutto næringsinntekt";
            case DODSDATO -> "Dødsdato";
            case DOKUMENTASJON_FORELIGGER -> "Dokumentasjon foreligger";
            case EKTEFELLES_BARN -> "Ektefelles/samboers barn";
            case ENDRING_NAERING -> "Endring i næringsvirksomhet";
            case ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD -> "Arbeidsforhold hos {value}";
            case ER_SOKER_BOSATT_I_NORGE -> "Bosted";
            case FODSELSVILKARET -> "Fødselsvilkåret";
            case FODSELSDATO -> "Fødselsdato";
            case FORDELING_FOR_ANDEL -> "{value}";
            case FORDELING_FOR_NY_ANDEL -> "{value}";
            case FORDELING_ETTER_BESTEBEREGNING -> "Fordeling etter besteberegning";
            case FORELDREANSVARSVILKARET -> "Foreldreansvarsvilkåret";
            case FRILANS_INNTEKT -> "Frilansinntekt";
            case FRILANSVIRKSOMHET -> "Frilansvirksomhet";
            case GYLDIG_MEDLEM_FOLKETRYGDEN -> "Vurder om søker har gyldig medlemskap i perioden";
            case INNTEKT_FRA_ARBEIDSFORHOLD -> "Inntekt fra {value}";
            case LØNNSENDRING_I_PERIODEN -> "Lønnsendring siste tre måneder";
            case MANN_ADOPTERER -> "Mann adopterer";
            case MOTTAR_YTELSE_ARBEID -> "Mottar søker ytelse for arbeid i {value}";
            case MOTTAR_YTELSE_FRILANS -> "Mottar søker ytelse for frilansaktiviteten";
            case MOTTATT_DATO -> "Dato for når søknaden kan anses som mottatt";
            case OMSORG -> "Omsorg";
            case OMSORGSOVERTAKELSESDATO -> "Omsorgsovertakelsesdato";
            case OMSORGSVILKAR -> "Omsorgsvilkåret";
            case IKKE_OMSORG_PERIODEN -> "Søker har ikke omsorg for barnet i perioden";
            case INNTEKTSKATEGORI_FOR_ANDEL -> "Inntektskategori for {value}";
            case OPPHOLDSRETT_EOS -> "Oppholdsrett";
            case OPPHOLDSRETT_IKKE_EOS -> "Lovlig opphold";
            case OVERSTYRT_BEREGNING -> "Overstyrt beregning:; Beløpet er endret fra";
            case OVERSTYRT_VURDERING -> "Overstyrt vurdering:; Utfallet er endret fra";
            case SELVSTENDIG_NÆRINGSDRIVENDE -> "Selvstendig næringsdrivende";
            case SOKERSOPPLYSNINGSPLIKT -> "Søkers opplysningsplikt";
            case SVANGERSKAPSPENGERVILKÅRET -> "Svangerskapsvilkåret";
            case SOKNADSFRIST -> "Søknadsfrist";
            case SOKNADSFRISTVILKARET -> "Søknadsfristvilkåret";
            case STARTDATO_FRA_SOKNAD -> "Startdato fra søknad";
            case TERMINBEKREFTELSE -> "Terminbekreftelse";
            case TERMINDATO -> "Termindato";
            case UTSTEDTDATO -> "Utstedt dato";
            case VILKAR_SOM_ANVENDES -> "Vilkår som anvendes";
            case FASTSETT_RESULTAT_PERIODEN -> "Resultat for perioden";
            case AVKLART_PERIODE -> "Avklart periode";
            case ANDEL_ARBEID -> "Andel i arbeid";
            case UTTAK_TREKKDAGER -> "Trekk";
            case UTTAK_STØNADSKONTOTYPE -> "Stønadskonto";
            case UTTAK_PERIODE_RESULTAT_TYPE -> "Resultatet";
            case UTTAK_PROSENT_UTBETALING -> "Utbetalingsgrad";
            case UTTAK_SAMTIDIG_UTTAK -> "Samtidig uttak";
            case UTTAK_TREKKDAGER_FLERBARN_KVOTE -> "Flerbarnsdager";
            case UTTAK_PERIODE_RESULTAT_ÅRSAK -> "Årsak resultat";
            case UTTAK_GRADERING_ARBEIDSFORHOLD -> "Gradering av arbeidsforhold";
            case UTTAK_GRADERING_AVSLAG_ÅRSAK -> "Årsak avslag gradering";
            case UTTAK_SPLITT_TIDSPERIODE -> endretFeltNavn.getNavn(); // Ikke i frontend
            case SYKDOM -> "Sykdom";
            case ARBEIDSFORHOLD -> "Arbeidsforhold hos {value}";
            case NY_FORDELING -> "Ny fordeling <b>{value}</b>";
            case NY_AKTIVITET -> "Det er lagt til ny aktivitet for <b>{value}</b>";
            case NYTT_REFUSJONSKRAV -> "Nytt refusjonskrav";
            case INNTEKT -> "Inntekt fra {value}";
            case INNTEKTSKATEGORI -> "Inntektskategori";
            case NAVN -> "Navn";
            case FNR -> "Fødselsnummer";
            case PERIODE_FOM -> "Periode f.o.m.";
            case PERIODE_TOM -> "Periode t.o.m.";
            case MANDAT -> "Mandat";
            case KONTAKTPERSON -> "Kontaktperson";
            case BRUKER_TVUNGEN -> "Søker er under tvungen forvaltning";
            case TYPE_VERGE -> "Type verge";
            case DAGPENGER_INNTEKT -> "Dagpenger";
            case KLAGE_RESULTAT_NFP -> "Resultat";
            case KLAGE_RESULTAT_KA -> "Ytelsesvedtak";
            case KLAGE_OMGJØR_ÅRSAK -> "Årsak til omgjøring";
            case ER_KLAGER_PART -> "Er klager part i saken";
            case ER_KLAGE_KONKRET -> "Klages det på konkrete elementer i vedtaket";
            case ER_KLAGEFRIST_OVERHOLDT -> "Er klagefristen overholdt";
            case ER_KLAGEN_SIGNERT -> "Er klagen signert";
            case PA_KLAGD_BEHANDLINGID -> "Vedtaket som er påklagd";
            case ANKE_RESULTAT -> "Vedtaket som er anket";
            case KONTROLL_AV_BESTEBEREGNING -> "Godkjenning av automatisk besteberegning";
            case ANKE_OMGJØR_ÅRSAK -> "Omgjøringsårsak";
            case ER_ANKER_IKKE_PART -> "Er anker ikke part";
            case ER_ANKE_IKKE_KONKRET -> "Er anke ikke konkret";
            case ER_ANKEFRIST_IKKE_OVERHOLDT -> "Er ankefrist ikke overholdt";
            case ER_ANKEN_IKKE_SIGNERT -> "Er anken ikke signer";
            case PA_ANKET_BEHANDLINGID -> "Vedtaket som er påklagd";
            case VURDER_ETTERLØNN_SLUTTPAKKE -> "Inntekt fra etterlønn eller sluttpakke";
            case FASTSETT_ETTERLØNN_SLUTTPAKKE -> "Inntekten";
            case ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT -> endretFeltNavn.getNavn(); // Ikke i frontend
            case ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON -> endretFeltNavn.getNavn(); // Ikke i frontend
            case FASTSETT_VIDERE_BEHANDLING -> endretFeltNavn.getNavn(); // Ikke i frontend
            case RETT_TIL_FORELDREPENGER -> "Rett til foreldrepenger";
            case MOR_MOTTAR_UFØRETRYGD -> "Mor mottar uføretrygd";
            case MOR_MOTTAR_STØNAD_EØS -> endretFeltNavn.getNavn(); // Ikke i frontend
            case ANNEN_FORELDER_RETT_EØS -> "Annen forelder har tilstrekkelig opptjening fra land i EØS";
            case VURDER_GRADERING_PÅ_ANDEL_UTEN_BG -> "Inntektsgrunnlag ved gradering";
            case DEKNINGSGRAD -> "Dekningsgrad";
            case TILBAKETREKK -> "Tilbaketrekk";
            case SAKSMARKERING -> "Saksmarkering";
            case INNHENT_SED -> "Innhent dokumentasjon";
            case HEL_TILRETTELEGGING_FOM -> "Hel tilrettelegging fra og med";
            case DELVIS_TILRETTELEGGING_FOM -> "Delvis tilrettelegging fra og med";
            case STILLINGSPROSENT -> "stillingsprosent";
            case SLUTTE_ARBEID_FOM -> "Slutte arbeid fra og med";
            case TILRETTELEGGING_BEHOV_FOM -> "Tilrettelegging er nødvendig fra og med";
            case TILRETTELEGGING_SKAL_BRUKES -> "Tilrettelegging skal brukes";
            case FARESIGNALER -> "Resultat";
            case MILITÆR_ELLER_SIVIL -> "Har søker militær- eller siviltjeneste i opptjeningsperioden";
            case NY_REFUSJONSFRIST -> "Utvidelse av frist for fremsatt refusjonskrav for {value}";
            case NY_STARTDATO_REFUSJON -> "Startdato for refusjon til {value}";
            case DELVIS_REFUSJON_FØR_STARTDATO -> "Delvis refusjon før ";
            case ORGANISASJONSNUMMER -> "Organisasjonsnummer";
            case ARBEIDSFORHOLD_BEKREFTET_TOM_DATO -> endretFeltNavn.getNavn(); // Ikke i frontend
            case ANKE_AVVIST_ÅRSAK -> "Avvisningsårsak";
            case AKTIVITETSKRAV_AVKLARING -> endretFeltNavn.getNavn(); // Ikke i frontend
            case UTTAKPERIODE_DOK_AVKLARING -> "Perioden {value}";
            case FAKTA_UTTAK_PERIODE -> "Perioden {value}";
            case SVP_OPPHOLD_PERIODE -> "Periode med opphold";
            case VURDERT_ETTERBETALING_TIL_SØKER -> "Vurdering av etterbetaling til søker";
            case UDEFINIERT -> throw new IllegalStateException("UDEFINTER ");
        };
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
            case AVVIS_KLAGE -> "Klagen er avvist";
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


    private static HistorikkinnslagDtoV2 fraMaltype2(Historikkinnslag h, UUID behandlingUUID) {

        return null; // TODO:
    }


    private static HistorikkinnslagDtoV2 fraMaltype1(Historikkinnslag innslag, UUID behandlingUUID) {
        var historikkinnslagDel = innslag.getHistorikkinnslagDeler().getFirst();
        var skjermlenke = historikkinnslagDel.getSkjermlenke().map(SkjermlenkeType::fraKode).orElse(null);
        var lenker = tilDto(innslag.getDokumentLinker());
        var begrunnelsetekst = begrunnelseFraDel(historikkinnslagDel).map(List::of);

        var body = begrunnelsetekst.orElse(List.of());
        return new HistorikkinnslagDtoV2(behandlingUUID, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(innslag.getAktør(), innslag.getOpprettetAv()),
            skjermlenke, innslag.getOpprettetTidspunkt(), lenker, innslag.getType().getNavn(), body);
    }

    private static Optional<String> begrunnelseFraDel(HistorikkinnslagDel historikkinnslagDel) {
        return historikkinnslagDel.getBegrunnelseFelt()
            .flatMap(HistorikkV2Adapter::finnÅrsakKodeListe)
            .map(Kodeverdi::getNavn)
            .or(historikkinnslagDel::getBegrunnelse);
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDto(List<HistorikkinnslagDokumentLink> dokumentLinker) {
        if (dokumentLinker == null) {
            return List.of();
        }
        return dokumentLinker.stream().map(d -> tilDto(d, null, null)) // TODO: husks aktiv journalpost og full URI
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDto(HistorikkinnslagDokumentLink lenke,
                                                          List<JournalpostId> journalPosterForSak,
                                                          URI dokumentPath) {
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        // dto.setUtgått(aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak));
        dto.setUtgått(true); //TODO
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
