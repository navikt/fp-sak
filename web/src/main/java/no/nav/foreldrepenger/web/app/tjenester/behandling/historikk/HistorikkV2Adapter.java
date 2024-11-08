package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_FOM;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_TOM;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType.TILBAKEKREVING_VIDEREBEHANDLING;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkDtoFellesMapper.leggTilAlleTeksterIHovedliste;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkDtoFellesMapper.tilHistorikkInnslagDto;

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
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
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
            case FORSLAG_VEDTAK,
                 FORSLAG_VEDTAK_UTEN_TOTRINN,
                 VEDTAK_FATTET,
                 // VEDTAK_FATTET_AUTOMATISK, que? fptilbake?
                 UENDRET_UTFALL,
                 REGISTRER_OM_VERGE
                -> fraMaltype2(h, behandlingUUID);
            case SAK_RETUR -> fraMaltype3(h, behandlingUUID);
            case AVBRUTT_BEH,
                BEH_KØET,
                BEH_VENT,
                IVERKSETTELSE_VENT,
                FJERNET_VERGE -> fraMalType4(h, behandlingUUID);
            case SAK_GODKJENT, // Inneholder også aksjonspunkt som ikke vises i frontend
                 FAKTA_ENDRET, KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, UTTAK, TERMINBEKREFTELSE_UGYLDIG, ANKE_BEH ->
                fraMalType5(h, behandlingUUID, journalPosterForSak, dokumentPath);
            case NY_INFO_FRA_TPS
                 //NY_GRUNNLAG_MOTTATT fptilbake
                -> fraMalType6(h, behandlingUUID);
            case OVERSTYRT-> fraMalType7(h, behandlingUUID, journalPosterForSak, dokumentPath);
            case OPPTJENING -> throw new IllegalStateException(String.format("Kode: %s har ingen maltype", h.getType())); // Ingen historikkinnslag for denne typen i DB!
            case OVST_UTTAK_SPLITT, FASTSATT_UTTAK_SPLITT, TILBAKEKREVING_VIDEREBEHANDLING -> fraMalType9(h, behandlingUUID);
            case OVST_UTTAK, FASTSATT_UTTAK -> fraMaltype10(h, behandlingUUID, journalPosterForSak, dokumentPath);
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

            default -> null;
        };
    }

    private static HistorikkinnslagDtoV2 fraMaltype1(Historikkinnslag innslag, UUID behandlingUUID, List<JournalpostId> journalPosterForSak, URI dokumentPath) {
        var del = innslag.getHistorikkinnslagDeler().getFirst();
        var begrunnelsetekst = begrunnelseFraDel(del).map(List::of);
        var body = begrunnelsetekst.orElse(List.of());
        return tilHistorikkInnslagDto(innslag, behandlingUUID, tilDokumentlenker(innslag.getDokumentLinker(), journalPosterForSak, dokumentPath), body);
    }

    private static HistorikkinnslagDtoV2 fraMaltype2(Historikkinnslag h, UUID behandlingUUID) {
        var del = h.getHistorikkinnslagDeler().getFirst();
        var hendelse = del.getHendelse().map(HistorikkDtoFellesMapper::fraHendelseFelt).orElseThrow();
        var tekst = del.getResultatFelt()
            .map(s -> String.format("%s: %s", hendelse, fraHistorikkResultat(s)))
            .orElse(hendelse);
        return tilHistorikkInnslagDto(h, behandlingUUID, List.of(tekst));
    }

    private static HistorikkinnslagDtoV2 fraMaltype3(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var aksjonspunkt = del.getTotrinnsvurderinger().stream()
                .map(HistorikkV2Adapter::fraAksjonspunktFelt)
                .flatMap(List::stream)
                .toList();

            leggTilAlleTeksterIHovedliste(tekster, aksjonspunkt);
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalType4(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var årsakTekst = del.getAarsakFelt().stream()
                .flatMap(felt -> finnÅrsakKodeListe(felt).stream())
                .map(Kodeverdi::getNavn)
                .toList();
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            leggTilAlleTeksterIHovedliste(tekster, årsakTekst, begrunnelsetekst);
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalType5(Historikkinnslag h, UUID behandlingUUID,
                                                     List<JournalpostId> journalPosterForSak,
                                                     URI dokumentPath) {
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var resultatTekst = del.getResultatFelt().stream()
                .map(HistorikkV2Adapter::fraHistorikkResultat)
                .toList();
            var gjeldendeFraInnslag = del.getGjeldendeFraFelt().stream()
                .map(felt -> tilGjeldendeFraInnslag(felt, del))
                .toList();
            var søknadsperiode = del.getAvklartSoeknadsperiode().stream()
                .map(HistorikkV2Adapter::fraSøknadsperiode)
                .flatMap(List::stream)
                .toList();
            var tema = del.getTema().stream()
                .map(HistorikkV2Adapter::fraTema)
                .flatMap(List::stream)
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

            leggTilAlleTeksterIHovedliste(tekster, resultatTekst, gjeldendeFraInnslag, søknadsperiode, tema, endretFelter,
                opplysninger, årsaktekst, begrunnelsetekst);
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tilDokumentlenker(h.getDokumentLinker(), journalPosterForSak, dokumentPath), tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalType6(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<String>();
        for (var del : h.getHistorikkinnslagDeler()) {
            var opplysninger = del.getOpplysninger().stream()
                .map(HistorikkV2Adapter::fraOpplysning)
                .toList();

            leggTilAlleTeksterIHovedliste(tekster, opplysninger);
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);

    }

    private static HistorikkinnslagDtoV2 fraMalType7(Historikkinnslag h, UUID behandlingUUID,
                                                     List<JournalpostId> journalPosterForSak,
                                                     URI dokumentPath) {
        var tekster = new ArrayList<String>();
        for (var del : h.getHistorikkinnslagDeler()) {
            // HENDELSE finnes ikke i DB for denne maltypen
            var resultatTekst = del.getResultatFelt().stream()
                .map(HistorikkV2Adapter::fraHistorikkResultat)
                .toList();
            var endretFelter = del.getEndredeFelt().stream()
                .map(HistorikkV2Adapter::fraEndretFeltMalType7)
                .toList();
            // OPPLYSNINGER finnes ikke i DB for denne maltypen
            var tema = del.getTema().stream() // Vises bare getNavnVerdi... gjør forbedring her
                .map(HistorikkV2Adapter::fraTema)
                .flatMap(List::stream)
                .toList();
            // AARSAK finnes ikke i DB for denne maltypen
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            leggTilAlleTeksterIHovedliste(tekster, resultatTekst, endretFelter, tema, begrunnelsetekst);
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tilDokumentlenker(h.getDokumentLinker(), journalPosterForSak, dokumentPath), tekster);
    }

    private static String fraEndretFeltMalType7(HistorikkinnslagFelt felt) {
        var fieldName = kodeverdiTilStreng(HistorikkEndretFeltType.fraKode(felt.getNavn()), felt.getNavnVerdi());
        var sub1 = fieldName.substring(0, fieldName.lastIndexOf(';'));
        var sub2 = fieldName.substring(fieldName.lastIndexOf(';') + 1);
        var fraVerdi = finnEndretFeltVerdi(felt, felt.getFraVerdi());
        var tilVerdi = finnEndretFeltVerdi(felt, felt.getTilVerdi());
        return "__{sub1}__ {sub2} __{fromValue}__ til __{toValue}__"
            .replace("{sub1}", sub1)
            .replace("{sub2}", sub2)
            .replace("{fromValue}", fraVerdi)
            .replace("{toValue}", tilVerdi);
    }

    private static HistorikkinnslagDtoV2 fraMalType9(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<String>();
        for (var del : h.getHistorikkinnslagDeler()) {
            var endretFeltTekst = h.getType().equals(TILBAKEKREVING_VIDEREBEHANDLING)
                ? fraEndretFeltMalType9Tilbakekr(del)
                : fraEndretFeltMalType9(h, del);
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            leggTilAlleTeksterIHovedliste(tekster, endretFeltTekst, begrunnelsetekst);
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);
    }

    private static HistorikkinnslagDtoV2 fraMaltype10(Historikkinnslag h, UUID behandlingUUID,
                                                      List<JournalpostId> journalPosterForSak,
                                                      URI dokumentPath) {
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
                opplysningTekst = String.format("__Overstyrt vurdering__ av perioden %s - %s.", periodeFom, periodeTom);

            }
            if (h.getType().equals(HistorikkinnslagType.FASTSATT_UTTAK)) {
                opplysningTekst = String.format("__Manuell vurdering__ av perioden %s - %s.", periodeFom, periodeTom);
            }

            var endretFelter = del.getEndredeFelt().stream()
                .map(HistorikkV2Adapter::fraEndretFeltMalType10)
                .toList();

            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            leggTilAlleTeksterIHovedliste(tekster, Collections.singletonList(opplysningTekst), endretFelter, begrunnelsetekst);
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tilDokumentlenker(h.getDokumentLinker(), journalPosterForSak, dokumentPath), tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalTypeAktivitetskrav(Historikkinnslag h, UUID behandlingUUID) {
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            var endretFelter = byggEndretFeltTekstForAktivitetskravMal(del);
            var begrunnelsetekst = begrunnelseFraDel(del).stream().toList();

            leggTilAlleTeksterIHovedliste(tekster, Collections.singletonList(endretFelter), begrunnelsetekst);
        }
        return tilHistorikkInnslagDto(h, behandlingUUID, tekster);
    }

    private static List<String> fraEndretFeltMalType9Tilbakekr(HistorikkinnslagDel del) {
        return del.getEndredeFelt().stream()
            .filter(felt -> !TilbakekrevingVidereBehandling.INNTREKK.getKode().equals(felt.getTilVerdi()))
            .map(HistorikkV2Adapter::fraEndretFeltMalType9Tilbakekr)
            .toList();
    }

    private static String fraEndretFeltMalType9Tilbakekr(HistorikkinnslagFelt felt) {
        var fieldName = kodeverdiTilStreng(HistorikkEndretFeltType.fraKode(felt.getNavn()), felt.getNavnVerdi());
        var verdi = finnEndretFeltVerdi(felt, felt.getTilVerdi());
        return "__{felt}__ er satt til __{verdi}__"
            .replace("{felt}", fieldName)
            .replace("{verdi}", verdi);
    }

    private static List<String> fraEndretFeltMalType9(Historikkinnslag h, HistorikkinnslagDel del) {
        var opprinneligPeriode = del.getEndredeFelt().getFirst().getFraVerdi();
        var numberOfPeriods = String.valueOf(del.getEndredeFelt().size());
        var splitPeriods = tekstRepresentasjonAvListe(del.getEndredeFelt());
        var tekst =  switch (h.getType()) {
            case OVST_UTTAK_SPLITT -> "__Overstyrt vurdering__ av perioden {opprinneligPeriode}.";
            case FASTSATT_UTTAK_SPLITT -> "__Manuell vurdering__ av perioden {opprinneligPeriode}.";
            default -> throw new IllegalStateException("Ikke støttet type" + h);
        };

        return List.of(
            tekst.replace("{opprinneligPeriode}", opprinneligPeriode),
            "__Perioden__ er delt i {numberOfPeriods} og satt til __{splitPeriods}__"
                .replace("{numberOfPeriods}", numberOfPeriods)
                .replace("{splitPeriods}", splitPeriods)
        );
    }

    private static String tekstRepresentasjonAvListe(List<HistorikkinnslagFelt> endretFelt) {
        StringBuilder tekst = new StringBuilder();
        var size = endretFelt.size();
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                tekst.append("og ").append(endretFelt.get(i).getTilVerdi());
            } else if (i == size - 2) {
                tekst.append(endretFelt.get(i).getTilVerdi()).append(" ");
            } else {
                tekst.append(endretFelt.get(i).getTilVerdi()).append(", ");
            }
        }
        return tekst.toString();

    }

    private static String byggEndretFeltTekstForAktivitetskravMal(HistorikkinnslagDel del) {
        var felt = del.getEndredeFelt().stream()
            .filter(e -> FeltNavnType.AKTIVITETSKRAV_AVKLARING.getKey().equals(e.getKlTilVerdi()))
            .findFirst()
            .orElseThrow();

        var tilVerdiNavn = FeltType.valueOf(felt.getTilVerdiKode()).name();
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


        if (felt.getFraVerdi() == null) {
            return String.format("Perioden __%s- %s__ er avklart til __%s__", periodeFom, periodeTom, tilVerdiNavn);
        } else {
            var fraVerdi = FeltType.valueOf(felt.getFraVerdiKode()).getText();
            return String.format("Perioden __%s - %s__ er endret fra %s til __%s__", periodeFom, periodeTom, fraVerdi, tilVerdiNavn);
        }
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

            tekst = String.format("__%s__ er endret fra %s uker og %s dager til __%s uker og %s dager__", fieldName, fraVerdiUker,
                fraVerdiDager, tilVerdiUker, tilVerdiDager);
        } else {
            tekst = historikkFraTilVerdi(felt, fieldName);
        }

        return tekst;
    }

    private static String historikkFraTilVerdi(HistorikkinnslagFelt felt, String fieldName) {
        var fraVerdi = finnEndretFeltVerdi(felt, felt.getFraVerdi());
        var tilVerdi = finnEndretFeltVerdi(felt, felt.getTilVerdi());
        var tekstMeldingTil = String.format("__%s__ er satt til __%s__", fieldName, tilVerdi);
        var tekstMeldingEndretFraTil = String.format("__%s__ er endret fra %s til __%s__", fieldName, fraVerdi, tilVerdi);
        var tekst = fraVerdi != null ? tekstMeldingEndretFraTil : tekstMeldingTil;

        if (HistorikkEndretFeltType.UTTAK_PROSENT_UTBETALING.getKode().equals(felt.getNavn()) && fraVerdi != null) {
            tekst = String.format("__%s__ er endret fra %s %% til __%s %%__", fieldName, fraVerdi, tilVerdi);
        } else if (HistorikkEndretFeltType.UTTAK_PROSENT_UTBETALING.getKode().equals(felt.getNavn())) {
            tekst = String.format("__%s__ er satt til __%s%%__", fieldName, tilVerdi);
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

    private static String fraOpplysning(HistorikkinnslagFelt opplysning) {
        var historikkOpplysningType = HistorikkOpplysningType.fraKode(opplysning.getNavn());

        return switch (historikkOpplysningType) {
            case ANTALL_BARN -> "__Antall barn__ som brukes i behandlingen: __{antallBarn}__".replace("{antallBarn}", opplysning.getTilVerdi()); // Brukes bare av maltype 5
            case TPS_ANTALL_BARN -> "Antall barn {verdi}".replace("{verdi}", opplysning.getTilVerdi());  // Brukes av maltype 6
            case FODSELSDATO -> "Når ble barnet født? {verdi}".replace("{verdi}", opplysning.getTilVerdi()); // Brukes av maltype 6
            //case UTTAK_PERIODE_FOM -> historikkOpplysningType.getNavn(); // Brukes av maltype 10 + aktivitetskrav
            //case UTTAK_PERIODE_TOM -> historikkOpplysningType.getNavn(); // Brukes av maltype 10 + aktivitetskrav
            default -> throw new IllegalStateException("Unexpected value: " + historikkOpplysningType);
        };
    }

    private static String finnEndretFeltVerdi(HistorikkinnslagFelt felt, String verdi) {
        if (verdi == null) {
            return null;
        }
        if (isBoolean(verdi)) {
            return konverterBoolean(verdi);
        }
        if (felt.getKlTilVerdi() != null) { // TODO: Henter tilVerdi uavhengig av fra og til... sikkert vært feil i frontend alltid.. og kanskje ikke relavnt heller
            try {
                return kodeverdiTilStrengEndretFeltTilverdi(verdi, verdi);
            } catch (IllegalStateException e) {
                return String.format("EndretFeltTypeTilVerdiKode %s finnes ikke-LEGG DET INN", felt.getTilVerdiKode());
            }
        }
        return verdi;
    }

    private static boolean isBoolean(String str) {
        return "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str);
    }

    private static String fraEndretFelt(HistorikkinnslagFelt felt) {
        var endretFeltNavn = HistorikkEndretFeltType.fraKode(felt.getNavn());

        var feltNavn = kodeverdiTilStreng(endretFeltNavn, felt.getNavnVerdi());

        var tilVerdi = konverterBoolean(felt.getTilVerdi());
        if (felt.getTilVerdi() != null && tilVerdi == null) {
            tilVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getTilVerdiKode(), felt.getTilVerdi());
        }

        if (felt.getFraVerdi() == null || endretFeltNavn.equals(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL)) {
            return String.format("__%s__ er satt til __%s__.", feltNavn, tilVerdi);
        }

        var fraVerdi = konverterBoolean(felt.getFraVerdi());
        if (fraVerdi == null) {
            fraVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getFraVerdiKode(), felt.getFraVerdi());
        }

        return String.format("__%s__ endret fra %s til __%s__", feltNavn, fraVerdi, tilVerdi);
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

    private static List<String> fraTema(HistorikkinnslagFelt tema) {
        var type = HistorikkEndretFeltType.fraKode(tema.getNavn());
        var tekst = switch (type) {
            case AKTIVITET -> "__Det er lagt til ny aktivitet:__"; // Finnes ikke frontend
            case FORDELING_FOR_NY_ANDEL -> "__Det er lagt til ny aktivitet:__";
            case FORDELING_FOR_ANDEL -> "Fordeling for __{value}__:";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        if (type.equals(HistorikkEndretFeltType.FORDELING_FOR_ANDEL)) {
            return List.of(tekst.replace("{value}", tema.getNavnVerdi()));
        } else {
            return List.of(
                tekst,
                String.format("__%s__", tema.getNavnVerdi())
            );
        }
    }

    private static List<String> fraSøknadsperiode(HistorikkinnslagFelt søknadsperiode) {
        var type = HistorikkAvklartSoeknadsperiodeType.fraKode(søknadsperiode.getNavn());

        var tekst = switch (type) {
            case GRADERING -> "__Uttak: gradering__";
            case UTSETTELSE_ARBEID -> "__Utsettelse: Arbeid__";
            case UTSETTELSE_FERIE -> "__Utsettelse: Ferie__";
            case UTSETTELSE_SKYDOM -> "__Utsettelse: Sykdom/skade__";
            case UTSETTELSE_HV -> "__Utsettelse: Heimevernet__";
            case UTSETTELSE_TILTAK_I_REGI_AV_NAV -> "__Utsettelse: Tiltak i regi av NAV__";
            case UTSETTELSE_INSTITUSJON_SØKER -> "__Utsettelse: Innleggelse av forelder__";
            case UTSETTELSE_INSTITUSJON_BARN -> "__Utsettelse: Innleggelse av barn__";
            case NY_SOEKNADSPERIODE -> "__Ny periode er lagt til__";
            case SLETTET_SOEKNASPERIODE -> "__Perioden er slettet__";
            case OVERFOERING_ALENEOMSORG -> "__Overføring: søker har aleneomsorg__";
            case OVERFOERING_SKYDOM -> "__Overføring: sykdom/skade__";
            case OVERFOERING_INNLEGGELSE -> "__Overføring: innleggelse__";
            case OVERFOERING_IKKE_RETT -> "__Overføring: annen forelder har ikke rett__";
            case UTTAK -> "__Uttak__";
            case OPPHOLD -> "__Opphold: annen foreldres uttak__";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        if (type.equals(HistorikkAvklartSoeknadsperiodeType.GRADERING)) {
            return List.of(
                tekst,
                søknadsperiode.getNavnVerdi(),
                søknadsperiode.getTilVerdi()
            );
        } else {
            return List.of(
                tekst,
                søknadsperiode.getTilVerdi()
            );
        }
    }

    private static String tilGjeldendeFraInnslag(HistorikkinnslagFelt gjeldendeFra, HistorikkinnslagDel del) {
        // Fra DB: Hverken navn, navnverdi, tilVerdi er null (FPSAK)
        var historikkEndretFeltType = HistorikkEndretFeltType.fraKode(gjeldendeFra.getNavn());

        var endretFeltTekst = switch (historikkEndretFeltType) {
            case NY_AKTIVITET -> "Det er lagt til ny aktivitet for __%s__";
            case NY_FORDELING -> "Ny fordeling __%s__";
            default -> throw new IllegalArgumentException();
        };
        // Historikk.Template.5.VerdiGjeldendeFra
        var verditekst = String.format(" gjeldende fra __%s__:", gjeldendeFra.getTilVerdi());

        return String.format(endretFeltTekst, gjeldendeFra.getNavnVerdi()) + verditekst + (del.getEndredeFelt()
            .isEmpty() ? "Ingen endring av vurdering" : "");
    }


    private static String fraHistorikkResultat(HistorikkinnslagFelt resultat) {
        if (resultat.getKlTilVerdi().equals(HistorikkResultatType.KODEVERK)) {
            var historikkResultatType = HistorikkResultatType.valueOf(resultat.getTilVerdiKode());
            return switch (historikkResultatType) {
                case MEDHOLD_I_KLAGE -> "Vedtaket er omgjort";
                case OPPHEVE_VEDTAK -> "Vedtaket er opphevet";
                case OPPRETTHOLDT_VEDTAK -> "Vedtaket er opprettholdt";
                case STADFESTET_VEDTAK -> "Vedtaket er stadfestet";
                case DELVIS_MEDHOLD_I_KLAGE -> "Vedtaket er delvis omgjort";
                case KLAGE_HJEMSENDE_UTEN_OPPHEVE -> "Behandling er hjemsendt";
                case UGUNST_MEDHOLD_I_KLAGE -> "Vedtaket er omgjort til ugunst";
                default -> historikkResultatType.getNavn(); // Resten lik enum
            };
        } else {
            return VedtakResultatType.valueOf(resultat.getTilVerdiKode()).getNavn(); // Alle like som enum navn
        }
    }

    private static Optional<String> begrunnelseFraDel(HistorikkinnslagDel historikkinnslagDel) {
        return historikkinnslagDel.getBegrunnelseFelt()
            .flatMap(HistorikkV2Adapter::finnÅrsakKodeListe)
            .map(Kodeverdi::getNavn)
            .or(historikkinnslagDel::getBegrunnelse);
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

    private static List<HistorikkInnslagDokumentLinkDto> tilDokumentlenker(List<HistorikkinnslagDokumentLink> dokumentLinker,
                                                                           List<JournalpostId> journalPosterForSak,
                                                                           URI dokumentPath) {
        if (dokumentLinker == null) {
            return List.of();
        }
        return dokumentLinker.stream().map(d -> tilDokumentlenker(d, journalPosterForSak, dokumentPath)) //
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDokumentlenker(HistorikkinnslagDokumentLink lenke,
                                                                     List<JournalpostId> journalPosterForSak,
                                                                     URI dokumentPath) {
        var erUtgått = aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak);
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(erUtgått ? String.format("%s (utgått)", lenke.getLinkTekst()) : lenke.getLinkTekst());
        dto.setUtgått(erUtgått);
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


    private static List<String> fraAksjonspunktFelt(HistorikkinnslagTotrinnsvurdering aksjonspunktFelt) {
        var aksjonspunktTekst = aksjonspunktFelt.getAksjonspunktDefinisjon().getNavn();
        if (aksjonspunktFelt.erGodkjent()) {
            return List.of(String.format("__%s er godkjent__", aksjonspunktTekst));
        } else {
            return List.of(
                String.format("__%s må vurderes på nytt__", aksjonspunktTekst),
                String.format("Kommentar: %s", aksjonspunktFelt.getBegrunnelse())
            );
        }
    }
}
