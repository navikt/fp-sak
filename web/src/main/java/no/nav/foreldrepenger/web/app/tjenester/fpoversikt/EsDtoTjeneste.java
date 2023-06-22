package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static no.nav.foreldrepenger.web.app.tjenester.fpoversikt.DtoTjenesteFelles.statusForSøknad;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class EsDtoTjeneste {

    private DtoTjenesteFelles felles;

    @Inject
    public EsDtoTjeneste(DtoTjenesteFelles felles) {
        this.felles = felles;
    }

    EsDtoTjeneste() {
        //CDI
    }

    public Sak hentSak(Fagsak fagsak) {
        if (fagsak.getYtelseType() != FagsakYtelseType.ENGANGSTØNAD) {
            throw new IllegalArgumentException("Forventer bare es fagsaker");
        }
        var aktørId = fagsak.getAktørId().getId();
        var saksnummer = fagsak.getSaksnummer().getVerdi();
        var gjeldendeVedtak = felles.finnGjeldendeVedtak(fagsak);
        var åpenYtelseBehandling = felles.hentÅpenBehandling(fagsak);
        var familieHendelse = felles.finnFamilieHendelse(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
        var erSakAvsluttet = felles.erAvsluttet(fagsak);
        var ikkeHenlagteBehandlinger = felles.finnIkkeHenlagteBehandlinger(fagsak);
        var aksjonspunkt = felles.finnAksjonspunkt(ikkeHenlagteBehandlinger);
        var mottatteSøknader = felles.finnRelevanteSøknadsdokumenter(fagsak);
        var alleVedtak = felles.finnVedtakForFagsak(fagsak);
        var søknader = finnEsSøknader(åpenYtelseBehandling, mottatteSøknader);
        var vedtak = finnEsVedtak(alleVedtak);
        return new EsSak(saksnummer, aktørId, familieHendelse, erSakAvsluttet, aksjonspunkt, søknader, vedtak);
    }

    private Set<EsSak.Vedtak> finnEsVedtak(Stream<BehandlingVedtak> vedtak) {
        return vedtak.map(v -> new EsSak.Vedtak(v.getVedtakstidspunkt())).collect(Collectors.toSet());
    }

    private static Set<EsSak.Søknad> finnEsSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader) {
        return mottatteSøknader.stream()
            .map(md -> {
                var status = statusForSøknad(åpenYtelseBehandling, md.getBehandlingId());
                return new EsSak.Søknad(status, md.getMottattTidspunkt());
            })
            .collect(Collectors.toSet());
    }
}
