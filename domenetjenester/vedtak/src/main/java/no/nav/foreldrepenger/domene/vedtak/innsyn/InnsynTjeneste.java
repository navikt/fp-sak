package no.nav.foreldrepenger.domene.vedtak.innsyn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingOpprettingTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class InnsynTjeneste {

    private BehandlingOpprettingTjeneste behandlingOpprettingTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private InnsynRepository innsynRepository;

    InnsynTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InnsynTjeneste(BehandlingOpprettingTjeneste behandlingOpprettingTjeneste,
                          FagsakRepository fagsakRepository,
                          BehandlingRepository behandlingRepository,
                          BehandlingsresultatRepository behandlingsresultatRepository,
                          InnsynRepository innsynRepository) {
        this.behandlingOpprettingTjeneste = behandlingOpprettingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.innsynRepository = innsynRepository;
    }

    public Behandling opprettManueltInnsyn(Saksnummer saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer)
            .orElseThrow(
                () -> new TekniskException("FP-148968", String.format("Finner ingen fagsak som kan gis innsyn for saksnummer: %s", saksnummer)));

        return behandlingOpprettingTjeneste.opprettBehandling(fagsak, BehandlingType.INNSYN);
    }

    public void lagreVurderInnsynResultat(Behandling behandling, InnsynEntitet innsynResultat) {
        var innsynType = innsynResultat.getInnsynResultatType();
        lagreBehandlingResultat(innsynType, behandling);

        var innsynEntitetOpt = innsynRepository.hentForBehandling(behandling.getId());
        var innsynBuilder = InnsynEntitet.InnsynBuilder.builder(innsynEntitetOpt.orElse(null))
            .medMottattDato(innsynResultat.getMottattDato())
            .medInnsynResultatType(innsynType);

        if (innsynEntitetOpt.isEmpty()) {
            innsynBuilder.medBegrunnelse(innsynResultat.getBegrunnelse()).medBehandlingId(behandling.getId());
        }
        innsynRepository.lagreInnsyn(innsynBuilder.build(), innsynResultat.getInnsynDokumenterOld());

    }

    private void lagreBehandlingResultat(InnsynResultatType innsynResultatType, Behandling behandling) {
        var eksisterendeBehandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        var builder = eksisterendeBehandlingsresultat.isPresent() ? Behandlingsresultat.builderEndreEksisterende(
            eksisterendeBehandlingsresultat.get()) : Behandlingsresultat.builderForInngangsvilkår();
        builder.medBehandlingResultatType(konverterResultatType(innsynResultatType));
        var res = builder.buildFor(behandling);
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(res.getVilkårResultat(), lås);
        behandlingRepository.lagre(behandling, lås);
    }

    private static BehandlingResultatType konverterResultatType(InnsynResultatType innsynResultatType) {
        // TODO (Maur): bør unngå to kodeverk for samme, evt. linke med Kodeliste relasjon eller abstrahere med interface
        if (InnsynResultatType.INNVILGET.equals(innsynResultatType)) {
            return BehandlingResultatType.INNSYN_INNVILGET;
        }
        if (InnsynResultatType.DELVIS_INNVILGET.equals(innsynResultatType)) {
            return BehandlingResultatType.INNSYN_DELVIS_INNVILGET;
        }
        if (InnsynResultatType.AVVIST.equals(innsynResultatType)) {
            return BehandlingResultatType.INNSYN_AVVIST;
        }
        throw new IllegalArgumentException("Utviklerfeil: Ukjent resultat-type");
    }


}
