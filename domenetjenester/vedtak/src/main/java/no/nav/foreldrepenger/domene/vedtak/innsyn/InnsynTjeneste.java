package no.nav.foreldrepenger.domene.vedtak.innsyn;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør.SAKSBEHANDLER;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@ApplicationScoped
public class InnsynTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private InnsynHistorikkTjeneste innsynHistorikkTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private InnsynRepository innsynRepository;

    InnsynTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InnsynTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                              InnsynHistorikkTjeneste innsynHistorikkTjeneste,
                              FagsakRepository fagsakRepository,
                              BehandlingRepository behandlingRepository,
                              BehandlingsresultatRepository behandlingsresultatRepository,
                              InnsynRepository innsynRepository,
                              BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.innsynHistorikkTjeneste = innsynHistorikkTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.innsynRepository = innsynRepository;
    }

    public Behandling opprettManueltInnsyn(Saksnummer saksnummer) {
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer)
            .orElseThrow(() -> InnsynFeil.FACTORY.tjenesteFinnerIkkeFagsakForInnsyn(saksnummer).toException());

        BehandlingType behandlingType = BehandlingType.INNSYN;
        Behandling nyBehandling = Behandling.nyBehandlingFor(fagsak, behandlingType).build();
        nyBehandling.setBehandlendeEnhet(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak));

        innsynHistorikkTjeneste.opprettHistorikkinnslag(nyBehandling, SAKSBEHANDLER);

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(nyBehandling);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, nyBehandling);

        return nyBehandling;
    }

    public void lagreVurderInnsynResultat(Behandling behandling, InnsynEntitet innsynResultat) {
        InnsynResultatType innsynType = innsynResultat.getInnsynResultatType();
        lagreBehandlingResultat(innsynType, behandling);

        Optional<InnsynEntitet> innsynEntitetOpt = innsynRepository.hentForBehandling(behandling.getId());
        InnsynEntitet.InnsynBuilder innsynBuilder = InnsynEntitet.InnsynBuilder.builder(innsynEntitetOpt.orElse(null))
            .medMottattDato(innsynResultat.getMottattDato())
            .medInnsynResultatType(innsynType);

        if (!innsynEntitetOpt.isPresent()) {
            innsynBuilder
                .medBegrunnelse(innsynResultat.getBegrunnelse())
                .medBehandlingId(behandling.getId());
        }
        innsynRepository.lagreInnsyn(innsynBuilder.build(), innsynResultat.getInnsynDokumenterOld());

    }

    private void lagreBehandlingResultat(InnsynResultatType innsynResultatType, Behandling behandling) {
        Optional<Behandlingsresultat> eksisterendeBehandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        Behandlingsresultat.Builder builder = eksisterendeBehandlingsresultat.isPresent()
            ? Behandlingsresultat.builderEndreEksisterende(eksisterendeBehandlingsresultat.get())
            : Behandlingsresultat.builderForInngangsvilkår();
        builder.medBehandlingResultatType(konverterResultatType(innsynResultatType));
        Behandlingsresultat res = builder.buildFor(behandling);
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(res.getVilkårResultat(), lås);
        behandlingRepository.lagre(behandling, lås);
    }

    private static BehandlingResultatType konverterResultatType(InnsynResultatType innsynResultatType) {
        // TODO (Maur): bør unngå to kodeverk for samme, evt. linke med Kodeliste relasjon eller abstrahere med interface
        if (InnsynResultatType.INNVILGET.equals(innsynResultatType)) {
            return BehandlingResultatType.INNSYN_INNVILGET;
        } else if (InnsynResultatType.DELVIS_INNVILGET.equals(innsynResultatType)) {
            return BehandlingResultatType.INNSYN_DELVIS_INNVILGET;
        } else if (InnsynResultatType.AVVIST.equals(innsynResultatType)) {
            return BehandlingResultatType.INNSYN_AVVIST;
        }
        throw new IllegalArgumentException("Utviklerfeil: Ukjent resultat-type");
    }


}
