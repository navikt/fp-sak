package no.nav.foreldrepenger.datavarehus.xml;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.vedtak.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.DvhPersonopplysningXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.OppdragXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlFeil;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.vedtak.v2.VedtakConstants;
import no.nav.vedtak.felles.integrasjon.felles.ws.JaxbHelper;
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
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
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
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(behandling.getFagsakId());
        Vedtak vedtak = factory.createVedtak();
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        vedtakXmlTjeneste.setVedtaksopplysninger(vedtak, fagsak, behandling);
        FagsakYtelseType ytelseType = fagsak.getYtelseType();
        String ikkeFunnet = "Ingen implementasjoner funnet for ytelse: " + ytelseType.getKode();
        
        FagsakYtelseTypeRef.Lookup.find(personopplysningXmlTjenester, ytelseType).orElseThrow(() -> new IllegalStateException(ikkeFunnet))
            .setPersonopplysninger(vedtak, behandlingId, behandling.getAktørId(), skjæringstidspunkter);
        
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
            throw VedtakXmlFeil.FACTORY.serialiseringsfeil(behandlingId, e).toException();
        }
    }
}
