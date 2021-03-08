package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarOpptjeningsvilkåretDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkåretDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkåretDto> {

    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;

    AvklarOpptjeningsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOpptjeningsvilkåretOppdaterer(OpptjeningRepository opptjeningRepository,
            BehandlingRepository behandlingRepository,
            BehandlingsresultatRepository behandlingsresultatRepository,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            HistorikkTjenesteAdapter historikkAdapter) {

        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOpptjeningsvilkåretDto dto, AksjonspunktOppdaterParameter param) {
        var nyttUtfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        var vilkårResultat = behandlingsresultatRepository.hent(param.getBehandlingId()).getVilkårResultat();

        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        if (nyttUtfall.equals(VilkårUtfallType.OPPFYLT)) {
            sjekkOmVilkåretKanSettesTilOppfylt(param.getBehandlingId());
            oppdaterUtfallOgLagre(behandling, vilkårResultat, nyttUtfall);
            return OppdateringResultat.utenOveropp();
        }

        oppdaterUtfallOgLagre(behandling, vilkårResultat, nyttUtfall);

        return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
    }

    private void oppdaterUtfallOgLagre(Behandling behandling, VilkårResultat vilkårResultat, VilkårUtfallType utfallType) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        var builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        if (utfallType.equals(VilkårUtfallType.OPPFYLT)) {
            builder.leggTilVilkårResultatManueltOppfylt(VilkårType.OPPTJENINGSVILKÅRET);
        } else {
            builder.leggTilVilkårResultatManueltIkkeOppfylt(VilkårType.OPPTJENINGSVILKÅRET, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING);
        }
        var resultat = builder.buildFor(behandling);
        behandlingRepository.lagre(resultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private void sjekkOmVilkåretKanSettesTilOppfylt(Long behandlingId) {
        final var opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.isPresent()) {
            final var antall = opptjening.get().getOpptjeningAktivitet().stream()
                    .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)).count();
            if (antall > 0) {
                return;
            }
        }
        throw new FunksjonellException("FP-093922", "Kan ikke sette opptjeningsvilkåret til oppfylt."
            + " Det må være minst en aktivitet for at opptjeningsvilkåret skal kunne settets til oppfylt.",
            "Sett på vent til det er mulig og manuelt legge inn aktiviteter ved overstyring.");
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, VilkårUtfallType nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
                .medEndretFelt(HistorikkEndretFeltType.OPPTJENINGSVILKARET, null, nyVerdi);

        var erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
                .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
                .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }
}
