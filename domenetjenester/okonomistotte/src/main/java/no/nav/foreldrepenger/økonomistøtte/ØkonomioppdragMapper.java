package no.nav.foreldrepenger.økonomistøtte;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.ObjectFactory;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Ompostering116;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragSkjemaConstants;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsLinje150;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TkodeStatusLinje;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.felles.ws.JaxbHelper;


public class ØkonomioppdragMapper {

    private static final String TYPE_ENHET = "BOS";
    private static final String ENHET = "8020";
    private static final LocalDate DATO_ENHET_FOM = LocalDate.of(1900, 1, 1);
    private static final String FRADRAG_TILLEGG = "T";

    // TODO (Team Tonic): Fjern global state oppdragskontroll
    private Oppdragskontroll oppdragskontroll;
    private ObjectFactory objectFactory;

    public ØkonomioppdragMapper(Oppdragskontroll okoOppdragskontroll) {
        this.oppdragskontroll = okoOppdragskontroll;
        this.objectFactory = new ObjectFactory();
    }

    public no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag mapVedtaksDataToOppdrag(Oppdrag110 okoOppdrag110) {
        final no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag oppdrag = objectFactory.createOppdrag();
        oppdrag.setOppdrag110(mapOppdrag110(okoOppdrag110));
        return oppdrag;
    }

    public List<String> generateOppdragXML() {
        List<Oppdrag110> oppdrag110UtenKvittering = oppdragskontroll.getOppdrag110Liste().stream()
            .filter(Oppdrag110::venterKvittering)
            .collect(Collectors.toList());

        List<String> oppdragXmlListe = new ArrayList<>();
        for (Oppdrag110 okoOppdrag110 : oppdrag110UtenKvittering) {
            no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag oppdrag = mapVedtaksDataToOppdrag(okoOppdrag110);
            try {
                String oppdragXml = JaxbHelper.marshalAndValidateJaxb(OppdragSkjemaConstants.JAXB_CLASS, oppdrag, OppdragSkjemaConstants.XSD_LOCATION);
                oppdragXmlListe.add(oppdragXml);
            } catch (JAXBException | SAXException e) {
                throw ØkonomistøtteFeil.FACTORY.xmlgenereringsfeil(oppdrag.getOppdrag110().getOppdragsId(), e).toException();
            }
        }
        return oppdragXmlListe;
    }

    private no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag110 mapOppdrag110(Oppdrag110 okoOppdrag110) {
        final no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag110 oppdrag110 = objectFactory.createOppdrag110();
        //TODO (Tonic): Løsningsbeskrivelse viser at Oppdrag110 er en liste men Økonomi Oppdrag tar bare et Oppdrag110
        String kode = okoOppdrag110.getKodeFagomrade();
        oppdrag110.setKodeAksjon(okoOppdrag110.getKodeAksjon());
        oppdrag110.setKodeEndring(okoOppdrag110.getKodeEndring());
        //TODO (Tonic): Sjekk vis dette må være enum eller ikke
        oppdrag110.setKodeFagomraade(okoOppdrag110.getKodeFagomrade());
        oppdrag110.setFagsystemId(String.valueOf(okoOppdrag110.getFagsystemId()));
        oppdrag110.setUtbetFrekvens(okoOppdrag110.getUtbetFrekvens());
        oppdrag110.setOppdragGjelderId(okoOppdrag110.getOppdragGjelderId());
        oppdrag110.setSaksbehId(String.valueOf(okoOppdrag110.getSaksbehId()));
        oppdrag110.setAvstemming115(mapAvstemming115(okoOppdrag110.getAvstemming()));

        oppdrag110.getOppdragsEnhet120().add(mapOppdragsEnhet120());
        oppdrag110.getOppdragsLinje150().addAll(mapOppdragsLinje150(okoOppdrag110.getOppdragslinje150Liste(), kode, okoOppdrag110.getSaksbehId()));
        oppdrag110.setDatoOppdragGjelderFom(toXmlGregCal(okoOppdrag110.getDatoOppdragGjelderFom()));

        Optional<no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116> optOmpostering116 = okoOppdrag110.getOmpostering116();
        optOmpostering116.ifPresent(ompostering116 -> oppdrag110.setOmpostering116(mapOmpostering116(ompostering116)));
        return oppdrag110;
    }


    private Ompostering116 mapOmpostering116(no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116 okoOmpostering116) {
        Ompostering116 ompostering116 = objectFactory.createOmpostering116();
        ompostering116.setOmPostering(okoOmpostering116.getOmPostering());
        ompostering116.setDatoOmposterFom(toXmlGregCal(okoOmpostering116.getDatoOmposterFom()));
        ompostering116.setSaksbehId(okoOmpostering116.getSaksbehId());
        ompostering116.setTidspktReg(okoOmpostering116.getTidspktReg());
        return ompostering116;
    }

    private no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Avstemming115 mapAvstemming115(Avstemming avstemming) {
        final no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Avstemming115 avstemming115 =
            objectFactory.createAvstemming115();

        avstemming115.setKodeKomponent(avstemming.getKodekomponent());
        avstemming115.setNokkelAvstemming(avstemming.getNøkkel());
        avstemming115.setTidspktMelding(avstemming.getTidspunkt());

        return avstemming115;
    }

    private no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsEnhet120 mapOppdragsEnhet120() {
        final no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsEnhet120 oppdragsEnhet120 =
            objectFactory.createOppdragsEnhet120();

        oppdragsEnhet120.setTypeEnhet(TYPE_ENHET);
        oppdragsEnhet120.setEnhet(ENHET);
        oppdragsEnhet120.setDatoEnhetFom(toXmlGregCal(DATO_ENHET_FOM));

        return oppdragsEnhet120;
    }

    private List<OppdragsLinje150> mapOppdragsLinje150(List<Oppdragslinje150> okoOppdrlinje150Liste, String kode, String saksbehId) {
        List<OppdragsLinje150> oppdragsLinje150Liste = new ArrayList<>();
        for (Oppdragslinje150 okoOppdrlinje150 : okoOppdrlinje150Liste) {
            OppdragsLinje150 oppdragsLinje150 = objectFactory.createOppdragsLinje150();
            oppdragsLinje150.setKodeEndringLinje(okoOppdrlinje150.getKodeEndringLinje());
            if (okoOppdrlinje150.gjelderOpphør()) {
                oppdragsLinje150.setKodeStatusLinje(TkodeStatusLinje.fromValue(okoOppdrlinje150.getKodeStatusLinje()));
            }
            if (okoOppdrlinje150.getDatoStatusFom() != null) {
                oppdragsLinje150.setDatoStatusFom(toXmlGregCal(okoOppdrlinje150.getDatoStatusFom()));
            }
            oppdragsLinje150.setVedtakId(okoOppdrlinje150.getVedtakId());
            oppdragsLinje150.setDelytelseId(String.valueOf(okoOppdrlinje150.getDelytelseId()));
            oppdragsLinje150.setKodeKlassifik(okoOppdrlinje150.getKodeKlassifik());
            oppdragsLinje150.setDatoVedtakFom(toXmlGregCal(okoOppdrlinje150.getDatoVedtakFom()));
            oppdragsLinje150.setDatoVedtakTom(toXmlGregCal(okoOppdrlinje150.getDatoVedtakTom()));
            oppdragsLinje150.setSats(new BigDecimal(okoOppdrlinje150.getSats()));
            oppdragsLinje150.setFradragTillegg(TfradragTillegg.fromValue(FRADRAG_TILLEGG));
            oppdragsLinje150.setTypeSats(okoOppdrlinje150.getTypeSats());
            oppdragsLinje150.setBrukKjoreplan(okoOppdrlinje150.getBrukKjoreplan());
            oppdragsLinje150.setSaksbehId(saksbehId);
            oppdragsLinje150.setUtbetalesTilId(okoOppdrlinje150.getUtbetalesTilId());
            oppdragsLinje150.setHenvisning(String.valueOf(okoOppdrlinje150.getHenvisning()));
            if (okoOppdrlinje150.getRefFagsystemId() != null) {
                oppdragsLinje150.setRefFagsystemId(String.valueOf(okoOppdrlinje150.getRefFagsystemId()));
            }
            if (okoOppdrlinje150.getRefDelytelseId() != null) {
                oppdragsLinje150.setRefDelytelseId(String.valueOf(okoOppdrlinje150.getRefDelytelseId()));
            }

            oppdragsLinje150.getAttestant180().add(mapAttestant180(saksbehId));
            if (!kode.equals(ØkonomiKodeFagområde.REFUTG.name())) {
                setGrad170OgRefusjonsinfo156(kode, okoOppdrlinje150, oppdragsLinje150);
            }
            oppdragsLinje150Liste.add(oppdragsLinje150);
        }
        return oppdragsLinje150Liste.stream()
            .sorted(Comparator.comparing(opp150 -> Long.parseLong(opp150.getDelytelseId())))
            .collect(Collectors.toList());
    }

    private void setGrad170OgRefusjonsinfo156(String kode, Oppdragslinje150 okoOppdrlinje150, OppdragsLinje150 oppdragsLinje150) {
        if (null != okoOppdrlinje150.getGrad()) {
            oppdragsLinje150.getGrad170().add(mapGrad170(okoOppdrlinje150.getGrad()));
        }
        if (ØkonomiKodeFagområde.gjelderRefusjonTilArbeidsgiver(kode)) {
            oppdragsLinje150.setRefusjonsinfo156(mapRefusjonInfo156(okoOppdrlinje150.getRefusjonsinfo156()));
        }
    }

    private no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Refusjonsinfo156 mapRefusjonInfo156(Refusjonsinfo156 okoRefusjonsInfo156) {
        final no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Refusjonsinfo156 refusjonsinfo156 =
            objectFactory.createRefusjonsinfo156();

        refusjonsinfo156.setMaksDato(toXmlGregCal(okoRefusjonsInfo156.getMaksDato()));
        refusjonsinfo156.setDatoFom(toXmlGregCal(okoRefusjonsInfo156.getDatoFom()));
        refusjonsinfo156.setRefunderesId(okoRefusjonsInfo156.getRefunderesId());

        return refusjonsinfo156;
    }

    private no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Grad170 mapGrad170(Utbetalingsgrad okoUtbetalingsgrad) {
        final no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Grad170 grad170 = objectFactory.createGrad170();

        grad170.setGrad(BigInteger.valueOf(okoUtbetalingsgrad.getVerdi()));
        grad170.setTypeGrad(okoUtbetalingsgrad.getType());

        return grad170;
    }

    private no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Attestant180 mapAttestant180(String saksbehId) {
        final no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Attestant180 attestant180 =
            objectFactory.createAttestant180();

        attestant180.setAttestantId(saksbehId);

        return attestant180;
    }

    private XMLGregorianCalendar toXmlGregCal(LocalDate dato) {
        return dato != null ? DateUtil.convertToXMLGregorianCalendarRemoveTimezone(dato) : null;
    }
}
