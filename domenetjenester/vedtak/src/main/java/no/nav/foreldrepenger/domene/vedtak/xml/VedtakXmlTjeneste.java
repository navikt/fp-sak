package no.nav.foreldrepenger.domene.vedtak.xml;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.felles.xml.felles.v2.KodeverksOpplysning;
import no.nav.vedtak.felles.xml.vedtak.v2.FagsakType;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;

@ApplicationScoped
public class VedtakXmlTjeneste {

    private SøknadRepository søknadRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;


    VedtakXmlTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VedtakXmlTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
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
        vedtak.setTema(VedtakXmlUtil.lagKodeverksOpplysning(Tema.FORELDRE_OG_SVANGERSKAPSPENGER));
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
        final var familieHendelse = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId()).map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).orElse(null);
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
        } else if( (FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType()))) {
            kodeverksOpplysning.setValue(FagsakType.FORELDREPENGER.value());
        }
        vedtak.setFagsakType(kodeverksOpplysning);
    }

    private void setFagsakId(Vedtak vedtak, Fagsak fagsak) {
        vedtak.setFagsakId(fagsak.getId().toString());
    }

    private void setFagsakAnnenForelder(Vedtak vedtak, Fagsak fagsak) {

        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak);

        if(fagsakRelasjon.isPresent()){
            if (fagsakRelasjon.get().getErAktivt()) {
                if (fagsak.getId().equals(fagsakRelasjon.get().getFagsakNrEn().getId()) && fagsakRelasjon.get().getFagsakNrTo().isPresent()) {
                    vedtak.setFagsakAnnenForelderId(fagsakRelasjon.get().getFagsakNrTo().get().getId().toString());
                } else if (fagsakRelasjon.get().getFagsakNrTo().isPresent() && fagsak.getId().equals(fagsakRelasjon.get().getFagsakNrTo().get().getId())) {
                    vedtak.setFagsakAnnenForelderId(fagsakRelasjon.get().getFagsakNrEn().getId().toString());
                }

            }
        }
    }

    private void setVedtaksdato(Behandling behandling, Vedtak vedtakKontrakt) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        vedtak.ifPresent(v -> vedtakKontrakt.setVedtaksdato(v.getVedtaksdato()));
    }

    private void setVedtaksResultat(Vedtak vedtakKontrakt, Behandling behandling) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        vedtak.ifPresent(v -> vedtakKontrakt.setVedtaksresultat((VedtakXmlUtil.lagKodeverksOpplysning(v.getVedtakResultatType()))));
    }
}
