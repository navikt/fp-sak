package no.nav.foreldrepenger.domene.vedtak.svp;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.domene.vedtak.xml.OppdragXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.økonomistøtte.HentOppdragMedPositivKvittering;
import no.nav.vedtak.felles.xml.vedtak.oppdrag.dvh.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.oppdrag.dvh.fp.v2.Oppdrag;
import no.nav.vedtak.felles.xml.vedtak.oppdrag.dvh.fp.v2.Oppdragslinje;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class OppdragXmlTjenesteImpl implements OppdragXmlTjeneste {
    private ObjectFactory oppdragObjectFactory;
    private HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering;

    public OppdragXmlTjenesteImpl() {
        //CDI
    }

    @Inject
    public OppdragXmlTjenesteImpl(HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering) {
        this.hentOppdragMedPositivKvittering = hentOppdragMedPositivKvittering;
        oppdragObjectFactory = new ObjectFactory();
    }

    @Override
    public void setOppdrag(Vedtak vedtak, Behandling behandling) {
        List<Oppdrag110> oppdrag110PositivKvittering = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvitteringFeilHvisVenter(behandling);
        if (!oppdrag110PositivKvittering.isEmpty()) {
            Oppdrag oppdragXml = oppdragObjectFactory.createOppdrag();
            for (Oppdrag110 oppdrag110 : oppdrag110PositivKvittering) {
                oppdragXml.setOppdragId(VedtakXmlUtil.lagLongOpplysning(oppdrag110.getId()));
                oppdragXml.setFagsystemId(VedtakXmlUtil.lagLongOpplysning(oppdrag110.getFagsystemId()));
                List<Oppdragslinje150> oppdragslinje150Liste = oppdrag110.getOppdragslinje150Liste();
                oppdragslinje150Liste.forEach(oppdragslinje150 -> oppdragXml.getOppdragslinje().add(konverterOppdragslinje(oppdragslinje150)));
            }
            vedtak.getOppdrag().add(oppdragXml);
        }
    }

    private Oppdragslinje konverterOppdragslinje(Oppdragslinje150 oppdragsLinje150) {
        Oppdragslinje oppdragslinje = new Oppdragslinje();
        oppdragslinje.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(oppdragsLinje150.getDatoVedtakFom(),oppdragsLinje150.getDatoVedtakTom()));
        oppdragslinje.setLinjeId(VedtakXmlUtil.lagLongOpplysning(oppdragsLinje150.getId()));
        oppdragslinje.setDelytelseId(VedtakXmlUtil.lagLongOpplysning(oppdragsLinje150.getDelytelseId()));
        Long refDelytelseId = oppdragsLinje150.getRefDelytelseId();
        VedtakXmlUtil.lagDateOpplysning(oppdragsLinje150.getDatoStatusFom()).ifPresent(oppdragslinje::setStatusFom);
        if (!Objects.isNull(oppdragsLinje150.getKodeStatusLinje())) {
            oppdragslinje.setKodeStatusLinje(VedtakXmlUtil.lagStringOpplysning(oppdragsLinje150.getKodeStatusLinje().getKode()));
        }
        if(refDelytelseId !=null) {
            oppdragslinje.setRefDelytelseId(VedtakXmlUtil.lagLongOpplysning(refDelytelseId));
        }
        oppdragslinje.setUtbetalesTilId(VedtakXmlUtil.lagStringOpplysning(oppdragsLinje150.getUtbetalesTilId()));
        oppdragslinje.setRefunderesId(VedtakXmlUtil.lagStringOpplysning(oppdragsLinje150.getRefusjonsinfo156() == null ? null : oppdragsLinje150.getRefusjonsinfo156().getRefunderesId()));
        return oppdragslinje;
    }
}

