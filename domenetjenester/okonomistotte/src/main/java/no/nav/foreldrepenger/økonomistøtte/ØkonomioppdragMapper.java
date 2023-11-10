package no.nav.foreldrepenger.økonomistøtte;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Attestant180;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Avstemming115;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Grad170;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.ObjectFactory;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Ompostering116;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragSkjemaConstants;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsEnhet120;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragsLinje150;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TkodeStatusLinje;
import no.nav.foreldrepenger.xmlutils.DateUtil;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.vedtak.exception.TekniskException;


// TODO: Flyttet til fp-ws-proxy og fjernet kvittering tester
public class ØkonomioppdragMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ØkonomioppdragMapper.class);

    private static final String TYPE_ENHET = "BOS";
    private static final String ENHET = "8020";
    private static final LocalDate DATO_ENHET_FOM = LocalDate.of(1900, 1, 1);
    private static final String FRADRAG_TILLEGG = "T";
    private static final String BRUK_KJOREPLAN = "N";
    private static final String TYPE_GRAD = "UFOR";
    private static final String KODE_AKSJON = "1";
    private static final String UTBET_FREKVENS = "MND";
    private static final LocalDate DATO_OPPDRAG_GJELDER_FOM = LocalDate.of(2000, 1, 1);

    private final ObjectFactory objectFactory = new ObjectFactory();

    Oppdrag mapVedtaksDataToOppdrag(Oppdrag110 okoOppdrag110, Long behandlingId) {
        var oppdrag = objectFactory.createOppdrag();
        oppdrag.setOppdrag110(mapOppdrag110(okoOppdrag110, behandlingId));
        return oppdrag;
    }

    public List<String> generateOppdragXML(Oppdragskontroll oppdragskontroll) {
        var oppdrag110UtenKvittering = oppdragskontroll.getOppdrag110Liste().stream()
            .filter(Oppdrag110::venterKvittering)
            .toList();

        List<String> oppdragXmlListe = new ArrayList<>();
        for (var okoOppdrag110 : oppdrag110UtenKvittering) {
            var oppdrag = mapVedtaksDataToOppdrag(okoOppdrag110, oppdragskontroll.getBehandlingId());
            LOG.debug("Oppretter oppdrag XML for behandling: {} og fagsystem: {}", oppdragskontroll.getBehandlingId(), okoOppdrag110.getFagsystemId());
            try {
                var oppdragXml = JaxbHelper.marshalAndValidateJaxb(OppdragSkjemaConstants.JAXB_CLASS, oppdrag, OppdragSkjemaConstants.XSD_LOCATION);
                oppdragXmlListe.add(oppdragXml);
            } catch (JAXBException | SAXException e) {
                throw new TekniskException("FP-536167",
                    String.format("Kan ikke konvertere oppdrag med id %s. Problemer ved generering av xml",
                        oppdrag.getOppdrag110().getOppdragsId()), e);
            }
        }
        return oppdragXmlListe;
    }

    private no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag110 mapOppdrag110(Oppdrag110 okoOppdrag110, Long behandlingId) {
        var oppdrag110 = objectFactory.createOppdrag110();
        var kodeFagområde = okoOppdrag110.getKodeFagomrade();

        oppdrag110.setKodeAksjon(KODE_AKSJON);
        oppdrag110.setKodeEndring(okoOppdrag110.getKodeEndring().name());
        oppdrag110.setKodeFagomraade(kodeFagområde.name());
        oppdrag110.setFagsystemId(String.valueOf(okoOppdrag110.getFagsystemId()));
        oppdrag110.setUtbetFrekvens(UTBET_FREKVENS);
        oppdrag110.setOppdragGjelderId(okoOppdrag110.getOppdragGjelderId());
        oppdrag110.setSaksbehId(String.valueOf(okoOppdrag110.getSaksbehId()));
        oppdrag110.setAvstemming115(mapAvstemming115(okoOppdrag110.getAvstemming()));

        oppdrag110.getOppdragsEnhet120().add(mapOppdragsEnhet120());
        oppdrag110.getOppdragsLinje150().addAll(mapOppdragsLinje150(okoOppdrag110.getOppdragslinje150Liste(), kodeFagområde, okoOppdrag110.getSaksbehId(), behandlingId));
        oppdrag110.setDatoOppdragGjelderFom(toXmlGregCal(DATO_OPPDRAG_GJELDER_FOM));

        var optOmpostering116 = okoOppdrag110.getOmpostering116();
        optOmpostering116.ifPresent(ompostering116 -> oppdrag110.setOmpostering116(mapOmpostering116(ompostering116, oppdrag110.getSaksbehId())));
        return oppdrag110;
    }


    private Ompostering116 mapOmpostering116(no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116 okoOmpostering116, String saksbehandlerId) {
        var ompostering116 = objectFactory.createOmpostering116();
        ompostering116.setOmPostering(okoOmpostering116.getOmPostering() ? "J" : "N");
        ompostering116.setDatoOmposterFom(toXmlGregCal(okoOmpostering116.getDatoOmposterFom()));
        ompostering116.setSaksbehId(saksbehandlerId);
        ompostering116.setTidspktReg(okoOmpostering116.getTidspktReg());
        return ompostering116;
    }

    private Avstemming115 mapAvstemming115(Avstemming avstemming) {
        var avstemming115 = objectFactory.createAvstemming115();

        avstemming115.setKodeKomponent(ØkonomiKodekomponent.VLFP.name());
        avstemming115.setNokkelAvstemming(avstemming.getNøkkel());
        avstemming115.setTidspktMelding(avstemming.getTidspunkt());

        return avstemming115;
    }

    private OppdragsEnhet120 mapOppdragsEnhet120() {
        var oppdragsEnhet120 = objectFactory.createOppdragsEnhet120();

        oppdragsEnhet120.setTypeEnhet(TYPE_ENHET);
        oppdragsEnhet120.setEnhet(ENHET);
        oppdragsEnhet120.setDatoEnhetFom(toXmlGregCal(DATO_ENHET_FOM));

        return oppdragsEnhet120;
    }

    private List<OppdragsLinje150> mapOppdragsLinje150(List<Oppdragslinje150> okoOppdrlinje150Liste, KodeFagområde kodeFagområde, String saksbehId, Long behandlingId) {
        List<OppdragsLinje150> oppdragsLinje150Liste = new ArrayList<>();
        for (var okoOppdrlinje150 : okoOppdrlinje150Liste) {
            var oppdragsLinje150 = objectFactory.createOppdragsLinje150();
            oppdragsLinje150.setKodeEndringLinje(okoOppdrlinje150.getKodeEndringLinje().name());
            if (okoOppdrlinje150.gjelderOpphør()) {
                oppdragsLinje150.setKodeStatusLinje(TkodeStatusLinje.fromValue(okoOppdrlinje150.getKodeStatusLinje().name()));
            }
            if (okoOppdrlinje150.getDatoStatusFom() != null) {
                oppdragsLinje150.setDatoStatusFom(toXmlGregCal(okoOppdrlinje150.getDatoStatusFom()));
            }
            oppdragsLinje150.setVedtakId(okoOppdrlinje150.getVedtakId());
            oppdragsLinje150.setDelytelseId(String.valueOf(okoOppdrlinje150.getDelytelseId()));
            oppdragsLinje150.setKodeKlassifik(okoOppdrlinje150.getKodeKlassifik().getKode());
            oppdragsLinje150.setDatoVedtakFom(toXmlGregCal(okoOppdrlinje150.getDatoVedtakFom()));
            oppdragsLinje150.setDatoVedtakTom(toXmlGregCal(okoOppdrlinje150.getDatoVedtakTom()));
            oppdragsLinje150.setSats(BigDecimal.valueOf(okoOppdrlinje150.getSats().getVerdi()));
            oppdragsLinje150.setFradragTillegg(TfradragTillegg.fromValue(FRADRAG_TILLEGG));
            oppdragsLinje150.setTypeSats(okoOppdrlinje150.getTypeSats().name());
            oppdragsLinje150.setBrukKjoreplan(BRUK_KJOREPLAN);
            oppdragsLinje150.setSaksbehId(saksbehId);
            oppdragsLinje150.setUtbetalesTilId(okoOppdrlinje150.getUtbetalesTilId());
            oppdragsLinje150.setHenvisning(String.valueOf(behandlingId));
            if (okoOppdrlinje150.getRefFagsystemId() != null) {
                oppdragsLinje150.setRefFagsystemId(String.valueOf(okoOppdrlinje150.getRefFagsystemId()));
            }
            if (okoOppdrlinje150.getRefDelytelseId() != null) {
                oppdragsLinje150.setRefDelytelseId(String.valueOf(okoOppdrlinje150.getRefDelytelseId()));
            }
            oppdragsLinje150.getAttestant180().add(mapAttestant180(saksbehId));

            if (!kodeFagområde.gjelderEngangsstønad()) {
                if (null != okoOppdrlinje150.getUtbetalingsgrad()) {
                    oppdragsLinje150.getGrad170().add(mapGrad170(okoOppdrlinje150.getUtbetalingsgrad()));
                }
                if (kodeFagområde.gjelderRefusjonTilArbeidsgiver()) {
                    oppdragsLinje150.setRefusjonsinfo156(mapRefusjonInfo156(okoOppdrlinje150.getRefusjonsinfo156()));
                }
            }

            oppdragsLinje150Liste.add(oppdragsLinje150);
        }
        return oppdragsLinje150Liste.stream()
            .sorted(Comparator.comparing(opp150 -> Long.parseLong(opp150.getDelytelseId())))
            .toList();
    }

    private no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Refusjonsinfo156 mapRefusjonInfo156(Refusjonsinfo156 okoRefusjonsInfo156) {
        var refusjonsinfo156 = objectFactory.createRefusjonsinfo156();

        refusjonsinfo156.setMaksDato(toXmlGregCal(okoRefusjonsInfo156.getMaksDato()));
        refusjonsinfo156.setDatoFom(toXmlGregCal(okoRefusjonsInfo156.getDatoFom()));
        refusjonsinfo156.setRefunderesId(okoRefusjonsInfo156.getRefunderesId());

        return refusjonsinfo156;
    }

    private Grad170 mapGrad170(Utbetalingsgrad okoUtbetalingsgrad) {
        var grad170 = objectFactory.createGrad170();

        grad170.setGrad(BigInteger.valueOf(okoUtbetalingsgrad.getVerdi()));
        grad170.setTypeGrad(TYPE_GRAD);

        return grad170;
    }

    private Attestant180 mapAttestant180(String saksbehId) {
        var attestant180 = objectFactory.createAttestant180();

        attestant180.setAttestantId(saksbehId);

        return attestant180;
    }

    private XMLGregorianCalendar toXmlGregCal(LocalDate dato) {
        return dato != null ? DateUtil.convertToXMLGregorianCalendarRemoveTimezone(dato) : null;
    }
}
