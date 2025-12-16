package no.nav.foreldrepenger.datavarehus.xml;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.felles.xml.felles.v2.KodeverksOpplysning;
import no.nav.vedtak.felles.xml.vedtak.v2.FagsakType;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;

@ApplicationScoped
public class VedtakXmlTjeneste {

    private SøknadRepository søknadRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;


    VedtakXmlTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VedtakXmlTjeneste(BehandlingRepositoryProvider repositoryProvider, FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
    }

    public void setVedtaksopplysninger(Vedtak vedtak, Fagsak fagsak, Behandling behandling) {
        setFagsakId(vedtak, fagsak);
        setFagsakAnnenForelder(vedtak, fagsak);
        setTema(vedtak);
        setFagsakType(vedtak, fagsak);
        setVedtaksdato(behandling, vedtak);
        setAnsvarligBeslutterIdent(vedtak, behandling);
        setAnsvarligSaksbehandlerIdent(vedtak, behandling);
        setBehandlendeEnhet(vedtak, behandling);
        setBehandlingsTema(vedtak, behandling);
        setKlagedato(vedtak, behandling);
        setSøknadsdato(vedtak, behandling);
        setVedtaksResultat(vedtak, behandling);
    }

    private void setTema(Vedtak vedtak) {
        // Anomali pga kode FOR_SVA mot DVH vs FOR i behandlingslager.Tema
        var kodeverksOpplysning = VedtakXmlUtil.lagTomKodeverksOpplysning();
        kodeverksOpplysning.setKode("FOR_SVA");
        kodeverksOpplysning.setValue(Tema.FOR.getNavn());
        kodeverksOpplysning.setKodeverk("TEMA");
        vedtak.setTema(kodeverksOpplysning);
    }

    private void setSøknadsdato(Vedtak vedtak, Behandling behandling) {
        var søknadOptional = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOptional.isPresent()) {
            var søknad = søknadOptional.get();
            vedtak.setSoeknadsdato(søknad.getSøknadsdato());
        }
    }

    private void setKlagedato(Vedtak vedtak, Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            vedtak.setKlagedato(behandling.getOpprettetDato().toLocalDate());
        }
    }

    private void setBehandlingsTema(Vedtak vedtak, Behandling behandling) {
        var familieHendelse = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .orElse(null);
        var behandlingTema = BehandlingTema.fraFagsak(behandling.getFagsak(), familieHendelse);
        vedtak.setBehandlingsTema(VedtakXmlUtil.lagKodeverksOpplysning(behandlingTema));
    }

    private void setBehandlendeEnhet(Vedtak vedtak, Behandling behandling) {
        vedtak.setBehandlendeEnhet(behandling.getBehandlendeEnhet());
    }

    private void setAnsvarligSaksbehandlerIdent(Vedtak vedtak, Behandling behandling) {
        vedtak.setAnsvarligSaksbehandlerIdent(behandling.getAnsvarligSaksbehandler());
    }

    private void setAnsvarligBeslutterIdent(Vedtak vedtak, Behandling behandling) {
        if (behandling.getAnsvarligBeslutter() != null) {
            vedtak.setAnsvarligBeslutterIdent(behandling.getAnsvarligBeslutter());
        }
    }

    private void setFagsakType(Vedtak vedtak, Fagsak fagsak) {
        var kodeverksOpplysning = new KodeverksOpplysning();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            kodeverksOpplysning.setValue(FagsakType.ENGANGSSTOENAD.value());
        } else if (FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType())) {
            kodeverksOpplysning.setValue(FagsakType.FORELDREPENGER.value());
        }
        vedtak.setFagsakType(kodeverksOpplysning);
    }

    private void setFagsakId(Vedtak vedtak, Fagsak fagsak) {
        vedtak.setFagsakId(fagsak.getId().toString());
    }

    private void setFagsakAnnenForelder(Vedtak vedtak, Fagsak fagsak) {
        fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(fagsakRelasjon -> fagsakRelasjon.getRelatertFagsak(fagsak))
            .ifPresent(rf -> vedtak.setFagsakAnnenForelderId(rf.getId().toString()));
    }

    private void setVedtaksdato(Behandling behandling, Vedtak vedtakKontrakt) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        vedtak.ifPresent(v -> vedtakKontrakt.setVedtaksdato(v.getVedtaksdato()));
    }

    private void setVedtaksResultat(Vedtak vedtakKontrakt, Behandling behandling) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        vedtak.ifPresent(v -> vedtakKontrakt.setVedtaksresultat(VedtakXmlUtil.lagKodeverksOpplysning(v.getVedtakResultatType())));
    }
}
