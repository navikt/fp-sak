package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.AksjonType;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Aksjonsdata;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.AvstemmingType;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Avstemmingsdata;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.DetaljType;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Fortegn;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.GrensesnittavstemmingSkjemaConstants;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Grunnlagsdata;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.KildeType;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.ObjectFactory;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Periodedata;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.grensesnittavstemming.Totaldata;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.vedtak.exception.TekniskException;

public class GrensesnittavstemmingMapper {
    private ObjectFactory objectFactory;
    private List<Oppdrag110> oppdragsliste;
    private String avstemmingId;
    protected static final String BRUKER_ID_FOR_VEDTAKSLØSNINGEN = "VL";
    private static final int DETALJER_PR_MELDING = 70;
    private KodeFagområde fagområde;

    public GrensesnittavstemmingMapper(List<Oppdrag110> oppdragsliste, KodeFagområde kodeFagområde) {
        if (oppdragsliste == null || oppdragsliste.isEmpty()) {
            throw new IllegalStateException("Grensesnittavstemming uten oppdragsliste er ikke mulig");
        }

        this.objectFactory = new ObjectFactory();
        this.avstemmingId = encodeUUIDBase64(UUID.randomUUID());
        this.fagområde = kodeFagområde;
        this.oppdragsliste = oppdragsliste.stream().filter(opp -> opp.getKodeFagomrade().equals(kodeFagområde)).toList();
    }

    private static String encodeUUIDBase64(UUID uuid) {
        var bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().encodeToString(bb.array()).substring(0, 22);
    }

    public String lagStartmelding() {
        return lagEnkeltAvstemmingsmelding(AksjonType.START);
    }

    public String lagSluttmelding() {
        return lagEnkeltAvstemmingsmelding(AksjonType.AVSL);
    }

    private String lagEnkeltAvstemmingsmelding(AksjonType aksjonType) {
        var avstemmingsdata = lagAvstemmingsdataFelles(aksjonType);
        return lagXmlMelding(avstemmingsdata);
    }

    public List<String> lagDatameldinger() {
        var avstemmingsdataListe = lagAvstemmingsdataListe();
        List<String> xmlMeldinger = new ArrayList<>(avstemmingsdataListe.size());
        for (var avstemmingsdata : avstemmingsdataListe) {
            xmlMeldinger.add(lagXmlMelding(avstemmingsdata));
        }
        return xmlMeldinger;
    }

    List<Avstemmingsdata> lagAvstemmingsdataListe() {
        List<Avstemmingsdata> liste = new ArrayList<>();
        var nesteOppdrag = 0;
        var totaldata = opprettTotaldata();
        var periodedata = opprettPeriodedata();
        var grunnlagsdata = opprettGrunnlagsdata();
        var første = true;
        while (nesteOppdrag < oppdragsliste.size()) {
            var avstemmingsdata = lagAvstemmingsdataFelles(AksjonType.DATA);
            if (første) {
                avstemmingsdata.setTotal(totaldata);
                avstemmingsdata.setPeriode(periodedata);
                avstemmingsdata.setGrunnlag(grunnlagsdata);
            }
            nesteOppdrag = opprettDetaljer(avstemmingsdata, nesteOppdrag);
            liste.add(avstemmingsdata);
            første = false;
        }
        return liste;
    }

    Avstemmingsdata lagAvstemmingsdataFelles(AksjonType aksjonType) {
        var avstemmingsdata = objectFactory.createAvstemmingsdata();
        avstemmingsdata.setAksjon(tilAksjonsdata(aksjonType));
        return avstemmingsdata;
    }

    private String lagXmlMelding(Avstemmingsdata avstemmingsdata) {
        try {
            return JaxbHelper.marshalAndValidateJaxb(GrensesnittavstemmingSkjemaConstants.JAXB_CLASS, avstemmingsdata,
                GrensesnittavstemmingSkjemaConstants.XSD_LOCATION);
        } catch (JAXBException | SAXException e) {
            throw new TekniskException("FP-531167", "Kan ikke opprette avstemmingsmelding. Problemer ved generering av xml", e);
        }
    }

    private Totaldata opprettTotaldata() {
        var totalBelop = 0L;
        for (var oppdrag : oppdragsliste) {
            totalBelop += getBelop(oppdrag);
        }
        var totaldata = objectFactory.createTotaldata();
        totaldata.setTotalAntall(oppdragsliste.size());
        totaldata.setTotalBelop(BigDecimal.valueOf(totalBelop));
        totaldata.setFortegn(tilFortegn(totalBelop));
        return totaldata;
    }

    private Fortegn tilFortegn(long belop) {
        return belop >= 0 ? Fortegn.T : Fortegn.F;
    }

    private long getBelop(Oppdrag110 oppdrag) {
        var belop = 0L;
        for (var oppdragslinje : oppdrag.getOppdragslinje150Liste()) {
            belop += oppdragslinje.getSats().getVerdi().longValue();
        }
        return belop;
    }

    private Periodedata opprettPeriodedata() {
        var periodedata = objectFactory.createPeriodedata();
        periodedata.setDatoAvstemtFom(tilPeriodeData(finnAvstemmingMedLavestNokkelAvstemmingsDato(oppdragsliste).getTidspunkt()));
        periodedata.setDatoAvstemtTom(tilPeriodeData(finnAvstemmingMedHøyestNokkelAvstemmingsDato(oppdragsliste).getTidspunkt()));
        return periodedata;
    }

    private Grunnlagsdata opprettGrunnlagsdata() {
        var godkjentAntall = 0;
        var godkjentBelop = 0L;
        var varselAntall = 0;
        var varselBelop = 0L;
        var avvistAntall = 0;
        var avvistBelop = 0L;
        var manglerAntall = 0;
        var manglerBelop = 0L;
        for (var oppdrag : oppdragsliste) {
            var belop = getBelop(oppdrag);
            var alvorlighetsgrad = oppdrag.erKvitteringMottatt() ? oppdrag.getOppdragKvittering().getAlvorlighetsgrad() : null;
            if (null == alvorlighetsgrad) {
                manglerBelop += belop;
                manglerAntall++;
            } else if (Alvorlighetsgrad.OK.equals(alvorlighetsgrad)) {
                godkjentBelop += belop;
                godkjentAntall++;
            } else if (Alvorlighetsgrad.OK_MED_MERKNAD.equals(alvorlighetsgrad)) {
                varselBelop += belop;
                varselAntall++;
            } else {
                avvistBelop += belop;
                avvistAntall++;
            }
        }
        var grunnlagsdata = objectFactory.createGrunnlagsdata();

        grunnlagsdata.setGodkjentAntall(godkjentAntall);
        grunnlagsdata.setGodkjentBelop(BigDecimal.valueOf(godkjentBelop));
        grunnlagsdata.setGodkjentFortegn(tilFortegn(godkjentBelop));

        grunnlagsdata.setVarselAntall(varselAntall);
        grunnlagsdata.setVarselBelop(BigDecimal.valueOf(varselBelop));
        grunnlagsdata.setVarselFortegn(tilFortegn(varselBelop));

        grunnlagsdata.setAvvistAntall(avvistAntall);
        grunnlagsdata.setAvvistBelop(BigDecimal.valueOf(avvistBelop));
        grunnlagsdata.setAvvistFortegn(tilFortegn(avvistBelop));

        grunnlagsdata.setManglerAntall(manglerAntall);
        grunnlagsdata.setManglerBelop(BigDecimal.valueOf(manglerBelop));
        grunnlagsdata.setManglerFortegn(tilFortegn(manglerBelop));

        return grunnlagsdata;
    }

    private int opprettDetaljer(Avstemmingsdata avstemmingsdata, int nesteOppdrag) {
        var oppdragNr = nesteOppdrag;
        while (DETALJER_PR_MELDING > avstemmingsdata.getDetalj().size() && oppdragNr < oppdragsliste.size()) {
            var oppdrag = oppdragsliste.get(oppdragNr);
            var alvorlighetsgrad = oppdrag.erKvitteringMottatt() ? oppdrag.getOppdragKvittering().getAlvorlighetsgrad() : null;
            if (null == alvorlighetsgrad) {
                opprettDetalj(avstemmingsdata, oppdrag, DetaljType.MANG, alvorlighetsgrad);
            } else if (Alvorlighetsgrad.OK.equals(alvorlighetsgrad)) {
                // ingen detaljer trenges.
            } else if (Alvorlighetsgrad.OK_MED_MERKNAD.equals(alvorlighetsgrad)) {
                opprettDetalj(avstemmingsdata, oppdrag, DetaljType.VARS, alvorlighetsgrad);
            } else {
                opprettDetalj(avstemmingsdata, oppdrag, DetaljType.AVVI, alvorlighetsgrad);
            }
            oppdragNr++;
        }
        return oppdragNr;
    }

    private void opprettDetalj(Avstemmingsdata avstemmingsdata, Oppdrag110 oppdrag110, DetaljType detaljType, Alvorlighetsgrad alvorlighetsgrad) {
        var kvittering = oppdrag110.getOppdragKvittering();
        String meldingKode = null;
        String beskrMelding = null;
        if (null != kvittering) {
            meldingKode = kvittering.getMeldingKode();
            beskrMelding = kvittering.getBeskrMelding();
        }
        var detaljdata = objectFactory.createDetaljdata();
        detaljdata.setDetaljType(detaljType);
        detaljdata.setOffnr(oppdrag110.getOppdragGjelderId());
        detaljdata.setAvleverendeTransaksjonNokkel(String.valueOf(oppdrag110.getFagsystemId()));
        detaljdata.setMeldingKode(meldingKode);
        detaljdata.setAlvorlighetsgrad(alvorlighetsgrad != null ? alvorlighetsgrad.getKode() : null);
        detaljdata.setTekstMelding(beskrMelding);
        detaljdata.setTidspunkt(oppdrag110.getAvstemming().getTidspunkt());
        avstemmingsdata.getDetalj().add(detaljdata);
    }

    private Aksjonsdata tilAksjonsdata(AksjonType aksjonType) {
        var aksjonsdata = objectFactory.createAksjonsdata();
        aksjonsdata.setAksjonType(aksjonType);
        aksjonsdata.setKildeType(KildeType.AVLEV);
        aksjonsdata.setAvstemmingType(AvstemmingType.GRSN);
        aksjonsdata.setAvleverendeKomponentKode(ØkonomiKodekomponent.VLFP.name());
        aksjonsdata.setMottakendeKomponentKode(ØkonomiKodekomponent.OS.name());
        aksjonsdata.setUnderkomponentKode(fagområde.name());
        aksjonsdata.setNokkelFom(finnAvstemmingMedLavestNokkelAvstemmingsDato(oppdragsliste).getNøkkel());
        var senestAvstemming = finnAvstemmingMedHøyestNokkelAvstemmingsDato(oppdragsliste);
        aksjonsdata.setNokkelTom(senestAvstemming.getNøkkel());
        aksjonsdata.setTidspunktAvstemmingTom(senestAvstemming.getTidspunkt());
        aksjonsdata.setAvleverendeAvstemmingId(avstemmingId);
        aksjonsdata.setBrukerId(BRUKER_ID_FOR_VEDTAKSLØSNINGEN);
        return aksjonsdata;
    }

    private Avstemming finnAvstemmingMedLavestNokkelAvstemmingsDato(List<Oppdrag110> oppdragsliste) {
        return oppdragsliste.stream()
            .map(Oppdrag110::getAvstemming)
            .min(Comparator.comparing(Avstemming::getNøkkel))
            .orElseThrow(() -> new IllegalStateException("Kan ikke finne NokkelFom for avstemming."));
    }

    private Avstemming finnAvstemmingMedHøyestNokkelAvstemmingsDato(List<Oppdrag110> oppdragsliste) {
        return oppdragsliste.stream()
            .map(Oppdrag110::getAvstemming)
            .max(Comparator.comparing(Avstemming::getNøkkel))
            .orElseThrow(() -> new IllegalStateException("Kan ikke finne NokkelTom for avstemming."));
    }

    private String tilPeriodeData(String localDateTimeString) {
        if (localDateTimeString == null || localDateTimeString.isEmpty()) {
            return null;
        }
        var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS");
        var periedeDateTime = LocalDateTime.parse(localDateTimeString, dateTimeFormatter);
        var dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
        return periedeDateTime.format(dateFormatter);
    }

    String getAvstemmingId() {
        return avstemmingId;
    }
}
