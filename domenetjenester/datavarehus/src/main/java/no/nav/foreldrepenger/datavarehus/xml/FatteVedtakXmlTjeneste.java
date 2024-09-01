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
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.vedtak.v2.VedtakConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.vedtak.felles.xml.vedtak.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;

@ApplicationScoped
public class FatteVedtakXmlTjeneste {

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private ObjectFactory factory;
    private VedtakXmlTjeneste vedtakXmlTjeneste;
    private Instance<PersonopplysningXmlTjeneste> personopplysningXmlTjenester;
    private BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjenester;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    FatteVedtakXmlTjeneste() {
        // For CDI
    }

    @Inject
    public FatteVedtakXmlTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                  VedtakXmlTjeneste vedtakXmlTjeneste,
                                  @Any Instance<PersonopplysningXmlTjeneste> personopplysningXmlTjenester,
                                  BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjenester,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.factory = new ObjectFactory();
        this.vedtakXmlTjeneste = vedtakXmlTjeneste;
        this.personopplysningXmlTjenester = personopplysningXmlTjenester;
        this.behandlingsresultatXmlTjenester = behandlingsresultatXmlTjenester;
    }

    public String opprettVedtakXml(Long behandlingId) {
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
