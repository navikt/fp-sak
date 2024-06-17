package no.nav.foreldrepenger.datavarehus.xml.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.OppdragXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.økonomistøtte.HentOppdragMedPositivKvittering;
import no.nav.vedtak.felles.xml.vedtak.oppdrag.dvh.es.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
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
        var oppdrag110PositivKvittering = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvitteringFeilHvisVenter(behandling);
        if (!oppdrag110PositivKvittering.isEmpty()) {
            var oppdragXml = oppdragObjectFactory.createOppdrag();

            var oppdrag110 = oppdrag110PositivKvittering.get(0);
            oppdragXml.setOppdragId(VedtakXmlUtil.lagLongOpplysning(oppdrag110.getId()));
            oppdragXml.setFagsystemId(VedtakXmlUtil.lagLongOpplysning(oppdrag110.getFagsystemId()));
            var oppdragslinje150 = oppdrag110.getOppdragslinje150Liste().get(0);

            oppdragXml.setLinjeId(VedtakXmlUtil.lagLongOpplysning(oppdragslinje150.getId()));

            if (oppdragslinje150.getDelytelseId() != null) {
                oppdragXml.setDelytelseId(VedtakXmlUtil.lagLongOpplysning(oppdragslinje150.getDelytelseId()));
            }

            if (oppdragslinje150.getRefDelytelseId() != null) {
                oppdragXml.setRefDelytelseId(VedtakXmlUtil.lagLongOpplysning(oppdragslinje150.getRefDelytelseId()));
            }

            oppdragXml.setUtbetalesTilId(VedtakXmlUtil.lagStringOpplysning(oppdragslinje150.getUtbetalesTilId()));
            oppdragXml.setRefunderesId(VedtakXmlUtil.lagStringOpplysning(
                oppdragslinje150.getRefusjonsinfo156() == null ? null : oppdragslinje150.getRefusjonsinfo156().getRefunderesId()));

            vedtak.getOppdrag().add(oppdragXml);
        }
    }
}

