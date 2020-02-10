package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.opptjening.Opptjeningsfeil;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarOpptjeningsvilkåretDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

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
        VilkårUtfallType nyttUtfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        VilkårResultat vilkårResultat = behandlingsresultatRepository.hent(param.getBehandlingId()).getVilkårResultat();

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
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
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        VilkårResultat.Builder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        if (utfallType.equals(VilkårUtfallType.OPPFYLT)) {
            builder.leggTilVilkårResultatManueltOppfylt(VilkårType.OPPTJENINGSVILKÅRET);
        } else {
            builder.leggTilVilkårResultatManueltIkkeOppfylt(VilkårType.OPPTJENINGSVILKÅRET, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING);
        }
        VilkårResultat resultat = builder.buildFor(behandling);
        behandlingRepository.lagre(resultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private void sjekkOmVilkåretKanSettesTilOppfylt(Long behandlingId) {
        final Optional<Opptjening> opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.isPresent()) {
            final long antall = opptjening.get().getOpptjeningAktivitet().stream()
                .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)).count();
            if (antall > 0) {
                return;
            }
        }
        throw Opptjeningsfeil.FACTORY.opptjeningPreconditionFailed().toException();
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, VilkårUtfallType nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.OPPTJENINGSVILKARET, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }
}
