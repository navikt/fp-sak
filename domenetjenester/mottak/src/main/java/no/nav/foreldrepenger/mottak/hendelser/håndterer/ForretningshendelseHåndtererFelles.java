package no.nav.foreldrepenger.mottak.hendelser.håndterer;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Kompletthetskontroller;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;

@Dependent
public class ForretningshendelseHåndtererFelles {

    private Behandlingsoppretter behandlingsoppretter;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private Kompletthetskontroller kompletthetskontroller;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private KøKontroller køKontroller;

    @SuppressWarnings("unused")
    private ForretningshendelseHåndtererFelles() {
        // For CDI
    }

    @Inject
    public ForretningshendelseHåndtererFelles(HistorikkinnslagTjeneste historikkinnslagTjeneste,
                                              Kompletthetskontroller kompletthetskontroller,
                                              BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                              Behandlingsoppretter behandlingsoppretter,
                                              FamilieHendelseTjeneste familieHendelseTjeneste,
                                              PersoninfoAdapter personinfoAdapter,
                                              KøKontroller køKontroller) {
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.kompletthetskontroller = kompletthetskontroller;
        this.behandlingsoppretter = behandlingsoppretter;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.køKontroller = køKontroller;
    }

    public Behandling opprettRevurderingLagStartTask(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var revurdering = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        return revurdering;
    }

    public void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        behandlingsoppretter.leggTilBehandlingsårsak(åpenBehandling, behandlingÅrsakType);
        historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(åpenBehandling, behandlingÅrsakType);
        kompletthetskontroller.vurderNyForretningshendelse(åpenBehandling, behandlingÅrsakType);
    }

    public void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<Behandling> køetBehandlingOpt) {
        if (køetBehandlingOpt.isPresent()) {
            // Oppdateringer fanges opp etter at behandling tas av kø, ettersom den vil passere steg innhentregisteropplysninger
            return;
        }
        var køetBehandling = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        historikkinnslagTjeneste.opprettHistorikkinnslagForVenteFristRelaterteInnslag(køetBehandling, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
        køKontroller.enkøBehandling(køetBehandling);
    }

    public boolean barnFødselogDødAlleredeRegistrert(Behandling åpenEllerForrige) {
        var familieHendelseGrunnlag = familieHendelseTjeneste.finnAggregat(åpenEllerForrige.getId()).orElse(null);
        if (familieHendelseGrunnlag == null) {
            return false;
        }
        var intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(åpenEllerForrige));
        var fødslerFraRegister = personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(åpenEllerForrige.getFagsakYtelseType(), åpenEllerForrige.getAktørId(), intervaller);
        var fødslerFraGrunnlag = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon().map(FamilieHendelseEntitet::getBarna).orElse(List.of());
        var grunnlagSammenlign = fødslerFraGrunnlag.stream().map(SammenlignBarn::new).toList();
        return fødslerFraRegister.size() == fødslerFraGrunnlag.size() &&
            fødslerFraRegister.stream().map(SammenlignBarn::new).allMatch(fbi -> grunnlagSammenlign.stream().anyMatch(ffg -> Objects.equals(fbi, ffg)));
    }

    private record SammenlignBarn(LocalDate fødselsdato, LocalDate dødsdato) {
        public SammenlignBarn(UidentifisertBarn barn) {
            this(barn.getFødselsdato(), barn.getDødsdato().orElse(null));
        }
        public SammenlignBarn(FødtBarnInfo barn) {
            this(barn.fødselsdato(), barn.getDødsdato().orElse(null));
        }
    }
}
