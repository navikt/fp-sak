package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveSendTilInfotrygdTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class BeregningInfotrygdsakTjeneste {
    private ProsessTaskRepository prosessTaskRepository;
    private FagsakRepository fagsakRepository;
    private VurderOmSakSkalTilInfotrygd vurderOmSakSkalTilInfotrygdTjeneste;

    protected BeregningInfotrygdsakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BeregningInfotrygdsakTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                            VurderOmSakSkalTilInfotrygd vurderOmSakSkalTilInfotrygdTjeneste,
                                            ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.vurderOmSakSkalTilInfotrygdTjeneste = vurderOmSakSkalTilInfotrygdTjeneste;
    }

    public boolean vurderOgOppdaterSakSomBehandlesAvInfotrygd(@Deprecated Behandling behandling, BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        AktørId aktørId = ref.getAktørId();
        if (vurderOmSakSkalTilInfotrygdTjeneste.skalForeldrepengersakBehandlesAvInfotrygd(ref.getSkjæringstidspunkt())) {
            fagsakRepository.fagsakSkalBehandlesAvInfotrygd(behandling.getFagsakId());
            oppdaterBeregningsgrunnlagvilkår(behandling);
            dispatchTilInfotrygd(behandling, behandlingId, aktørId);
            return true;
        }
        return false;
    }

    private void dispatchTilInfotrygd(Behandling behandling, Long behandlingId, AktørId aktørId) {
        ProsessTaskData data = new ProsessTaskData(OpprettOppgaveSendTilInfotrygdTask.TASKTYPE);
        data.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        prosessTaskRepository.lagre(data);
    }

    private void oppdaterBeregningsgrunnlagvilkår(Behandling behandling) {
        // TODO: oppdatering av vilkårresultat bør skje til slutt
        VilkårResultat.Builder builder = VilkårResultat
            .builderFraEksisterende(behandling.getBehandlingsresultat().getVilkårResultat())
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .leggTilVilkårResultat(
                VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
                VilkårUtfallType.IKKE_OPPFYLT,
                null,
                null,
                Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN,
                false,
                false,
                null,
                null);
        builder.buildFor(behandling);
    }
}
