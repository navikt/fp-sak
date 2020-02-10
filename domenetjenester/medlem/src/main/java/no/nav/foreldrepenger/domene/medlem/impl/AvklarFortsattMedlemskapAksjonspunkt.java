package no.nav.foreldrepenger.domene.medlem.impl;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.medlem.api.AvklarFortsattMedlemskapAksjonspunktDto;
import no.nav.foreldrepenger.domene.medlem.api.BekreftedePerioderAdapter;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class AvklarFortsattMedlemskapAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    public AvklarFortsattMedlemskapAksjonspunkt(BehandlingRepositoryProvider repositoryProvider, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    public void oppdater(Long behandlingId, AvklarFortsattMedlemskapAksjonspunktDto adapter) {
        List<BekreftedePerioderAdapter> perioder = adapter.getPerioder();
        VurdertMedlemskapPeriodeEntitet.Builder vurdertMedlemskapPeriode = medlemskapRepository.hentBuilderFor(behandlingId);
        LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();

        perioder.forEach(vurderingsperiode -> {
            // TODO(OJR) ønsker ikke og se på denne perioden hvis den blir sendt ned fra GUI
            if (!skjæringstidspunkt.equals(vurderingsperiode.getVurderingsdato())) {
                VurdertLøpendeMedlemskapBuilder løpendeBuilder = vurdertMedlemskapPeriode.getBuilderFor(vurderingsperiode.getVurderingsdato());
                List<String> aksjonspunkter = vurderingsperiode.getAksjonspunkter();
                if (aksjonspunkter.contains(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD.getKode())) {
                    håndterAksjonspunkt5019(løpendeBuilder, vurderingsperiode, behandlingId);
                }
                if (aksjonspunkter.contains(AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT.getKode())) {
                    håndterAksjonspunkt5023(løpendeBuilder, vurderingsperiode, behandlingId);
                }
                if (aksjonspunkter.contains(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT.getKode())) {
                    håndterAksjonspunkt5020(løpendeBuilder, vurderingsperiode, behandlingId);
                }
                if (aksjonspunkter.contains(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE.getKode())) {
                    håndterAksjonspunkt5021(løpendeBuilder, vurderingsperiode, behandlingId);
                }
                løpendeBuilder.medBegrunnelse(vurderingsperiode.getBegrunnelse());
                vurdertMedlemskapPeriode.leggTil(løpendeBuilder);
            }
        });
        medlemskapRepository.lagreLøpendeMedlemskapVurdering(behandlingId, vurdertMedlemskapPeriode.build());
    }

    // AVKLAR_LOVLIG_OPPHOLD
    private void håndterAksjonspunkt5019(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId) {
        Boolean nyVerdi = data.getLovligOppholdVurdering();
        Boolean tidligereVerdi = builder.build().getLovligOppholdVurdering();

        builder.medOppholdsrettVurdering(data.getOppholdsrettVurdering())
            .medLovligOppholdVurdering(nyVerdi)
            .medErEosBorger(data.getErEosBorger());

        lagHistorikkFor(data.getBegrunnelse())
            .medEndretFelt(HistorikkEndretFeltType.OPPHOLDSRETT_IKKE_EOS,
                mapTilLovligOppholdVerdiKode(tidligereVerdi),
                mapTilLovligOppholdVerdiKode(nyVerdi));
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    // AVKLAR_OPPHOLDSRETT
    private void håndterAksjonspunkt5023(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId) {
        Boolean nyVerdi = data.getOppholdsrettVurdering();
        Boolean tidligereVerdi = builder.build().getOppholdsrettVurdering();

        builder.medOppholdsrettVurdering(nyVerdi)
            .medLovligOppholdVurdering(data.getLovligOppholdVurdering())
            .medErEosBorger(data.getErEosBorger());

        lagHistorikkFor(data.getBegrunnelse())
            .medEndretFelt(HistorikkEndretFeltType.OPPHOLDSRETT_EOS, mapTilOppholdsrettVerdiKode(tidligereVerdi), mapTilOppholdsrettVerdiKode(nyVerdi));
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    // AVKLAR_OM_ER_BOSATT
    private void håndterAksjonspunkt5020(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId) {
        Boolean tidligereVerdi = builder.build().getBosattVurdering();
        Boolean nyVerdi = data.getBosattVurdering();

        builder.medBosattVurdering(nyVerdi);
        lagHistorikkFor(data.getBegrunnelse()).medEndretFelt(HistorikkEndretFeltType.ER_SOKER_BOSATT_I_NORGE, tidligereVerdi, nyVerdi);
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    // AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE
    private void håndterAksjonspunkt5021(VurdertLøpendeMedlemskapBuilder builder, BekreftedePerioderAdapter data, Long behandlingId) {
        MedlemskapManuellVurderingType manuellVurdering = builder.build().getMedlemsperiodeManuellVurdering();
        String tidligereVerdi = manuellVurdering != null ? manuellVurdering.getNavn() : null;
        String nyVerdi = data.getMedlemskapManuellVurderingType().getNavn();

        builder.medMedlemsperiodeManuellVurdering(data.getMedlemskapManuellVurderingType());
        lagHistorikkFor(data.getBegrunnelse()).medEndretFelt(HistorikkEndretFeltType.GYLDIG_MEDLEM_FOLKETRYGDEN, tidligereVerdi, nyVerdi);
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private HistorikkInnslagTekstBuilder lagHistorikkFor(String begrunnelse) {
        return historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP)
            .medBegrunnelse(begrunnelse);
    }

    private HistorikkEndretFeltVerdiType mapTilOppholdsrettVerdiKode(Boolean harOppholdsrett) {
        if (harOppholdsrett == null) {
            return null;
        }
        return harOppholdsrett ? HistorikkEndretFeltVerdiType.OPPHOLDSRETT : HistorikkEndretFeltVerdiType.IKKE_OPPHOLDSRETT;
    }

    private HistorikkEndretFeltVerdiType mapTilLovligOppholdVerdiKode(Boolean harLovligOpphold) {
        if (harLovligOpphold == null) {
            return null;
        }
        return harLovligOpphold ? HistorikkEndretFeltVerdiType.LOVLIG_OPPHOLD : HistorikkEndretFeltVerdiType.IKKE_LOVLIG_OPPHOLD;
    }
}
