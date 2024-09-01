package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class FastsettUttakManueltAksjonspunktUtlederTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final UttakRevurderingTestUtil testUtil = new UttakRevurderingTestUtil(repositoryProvider, iayTjeneste);

    private final FastsettUttakManueltAksjonspunktUtleder utleder = new FastsettUttakManueltAksjonspunktUtleder(repositoryProvider);


    @Test
    void skal_utlede_aksjonspunkt_for_klage_når_behandling_er_manuelt_opprettet_med_klageårsak() {
        var revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        lagreUttak(revurdering.getId());
        var input = lagInput(revurdering).medBehandlingManueltOpprettet(true)
            .medBehandlingÅrsaker(Set.of(BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(input);

        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE);
    }

    private void lagreUttak(Long id) {
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(id, new UttakResultatPerioderEntitet().leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(
            LocalDate.now(), LocalDate.now()).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build()));
    }

    private UttakInput lagInput(Behandling behandling) {
        return lagInput(behandling, false);
    }

    private UttakInput lagInput(Behandling behandling, boolean dødsfall) {
        var ytelsespesifiktGrunnlag = new ForeldrepengerGrunnlag().medDødsfall(dødsfall).medFamilieHendelser(
            new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(LocalDate.now(), null, List.of(), 1)));
        return new UttakInput(BehandlingReferanse.fra(behandling), null, iayTjeneste.hentGrunnlag(behandling.getId()), ytelsespesifiktGrunnlag);
    }

    @Test
    void skal_utlede_aksjonspunkt_for_død_når_behandling_er_manuelt_opprettet_med_dødsårsak() {
        var revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        lagreUttak(revurdering.getId());
        var input = lagInput(revurdering, true)
            .medBehandlingÅrsaker(Set.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD))
            .medBehandlingManueltOpprettet(true);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(input);

        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);
    }

    @Test
    void skal_utlede_aksjonspunkt_for_død_når_grunnlaget_har_opplysninger_om_død() {
        var revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        lagreUttak(revurdering.getId());
        var input = lagInput(revurdering, true);

        var aksjonspunkter = utleder.utledAksjonspunkterFor(input);

        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);
    }

    @Test
    void skal_utlede_aksjonspunkt_for_søknadsfrist_når_behandling_er_manuelt_opprettet_med_søknadsfristårsak() {
        var revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        lagreUttak(revurdering.getId());
        var input = lagInput(revurdering).medBehandlingManueltOpprettet(true)
            .medBehandlingÅrsaker(Set.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKNAD_FRIST));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(input);

        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST);
    }

    @Test
    void skal_ikke_utlede_aksjonspunkter_når_ingen_av_kriteriene_er_oppfylt() {
        var revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        lagreUttak(revurdering.getId());

        var aksjonspunkter = utleder.utledAksjonspunkterFor(lagInput(revurdering));

        assertThat(aksjonspunkter).isEmpty();
    }

}
