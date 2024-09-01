package no.nav.foreldrepenger.datavarehus.xml;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.vedtak.v2.VedtakConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.vedtak.felles.xml.vedtak.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;

@ApplicationScoped
public class DvhVedtakXmlTjeneste {

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private ObjectFactory factory;
    private VedtakXmlTjeneste vedtakXmlTjeneste;
    private Instance<OppdragXmlTjeneste> oppdragXmlTjenester;
    private Instance<DvhPersonopplysningXmlTjeneste> personopplysningXmlTjenester;
    private BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjenester;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    DvhVedtakXmlTjeneste() {
        // for CDI proxy
    }

    @Inject
    public DvhVedtakXmlTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                VedtakXmlTjeneste vedtakXmlTjeneste,
                                @Any Instance<DvhPersonopplysningXmlTjeneste> personopplysningXmlTjenester,
                                @Any Instance<OppdragXmlTjeneste> oppdragXmlTjenester,
                                BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjenester,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                StønadsstatistikkTjeneste stønadsstatistikkTjeneste) {
        this.vedtakXmlTjeneste = vedtakXmlTjeneste;
        this.oppdragXmlTjenester = oppdragXmlTjenester;
        this.personopplysningXmlTjenester = personopplysningXmlTjenester;
        this.behandlingsresultatXmlTjenester = behandlingsresultatXmlTjenester;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;

        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.factory = new ObjectFactory();
    }

    public String opprettDvhVedtakXml(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = fagsakRepository.finnEksaktFagsak(behandling.getFagsakId());
        var vedtak = factory.createVedtak();
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        vedtakXmlTjeneste.setVedtaksopplysninger(vedtak, fagsak, behandling);
        var ytelseType = fagsak.getYtelseType();
        var ikkeFunnet = "Ingen implementasjoner funnet for ytelse: " + ytelseType.getKode();

        FagsakYtelseTypeRef.Lookup.find(personopplysningXmlTjenester, ytelseType).orElseThrow(() -> new IllegalStateException(ikkeFunnet))
            .setPersonopplysninger(vedtak, ref, skjæringstidspunkter);

        behandlingsresultatXmlTjenester.setBehandlingresultat(vedtak, behandling);

        FagsakYtelseTypeRef.Lookup.find(oppdragXmlTjenester, ytelseType).orElseThrow(() -> new IllegalStateException(ikkeFunnet))
            .setOppdrag(vedtak, behandling);

        return genererXml(behandlingId, vedtak);
    }

    private String genererXml(Long behandlingId, Vedtak vedtak) {
        try {
            return JaxbHelper.marshalAndValidateJaxb(VedtakConstants.JAXB_CLASS,
                vedtak,
                VedtakConstants.XSD_LOCATION,
                VedtakConstants.ADDITIONAL_XSD_LOCATIONS,
                VedtakConstants.ADDITIONAL_CLASSES);
        } catch (JAXBException | SAXException e) {
            throw VedtakXmlFeil.serialiseringsfeil(behandlingId, e);
        }
    }
}
