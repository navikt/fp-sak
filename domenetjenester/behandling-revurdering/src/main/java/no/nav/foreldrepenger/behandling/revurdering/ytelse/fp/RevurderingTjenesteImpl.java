package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class RevurderingTjenesteImpl implements RevurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private PleiepengerRepository pleiepengerRepository;
    private UføretrygdRepository uføretrygdRepository;
    private NesteSakRepository nesteSakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private OpptjeningIUtlandDokStatusRepository opptjeningIUtlandDokStatusRepository;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private RevurderingEndring revurderingEndring;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VergeRepository vergeRepository;
    private SøknadRepository søknadRepository;

    public RevurderingTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                   BehandlingGrunnlagRepositoryProvider grunnlagProvider,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   InntektArbeidYtelseTjeneste iayTjeneste,
                                   @FagsakYtelseTypeRef("FP") RevurderingEndring revurderingEndring,
                                   RevurderingTjenesteFelles revurderingTjenesteFelles,
                                   VergeRepository vergeRepository) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.ytelsesFordelingRepository = grunnlagProvider.getYtelsesFordelingRepository();
        this.familieHendelseRepository = grunnlagProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = grunnlagProvider.getPersonopplysningRepository();
        this.medlemskapRepository = grunnlagProvider.getMedlemskapRepository();
        this.pleiepengerRepository = grunnlagProvider.getPleiepengerRepository();
        this.uføretrygdRepository = grunnlagProvider.getUføretrygdRepository();
        this.opptjeningIUtlandDokStatusRepository = grunnlagProvider.getOpptjeningIUtlandDokStatusRepository();
        this.nesteSakRepository = grunnlagProvider.getNesteSakRepository();
        this.revurderingEndring = revurderingEndring;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.vergeRepository = vergeRepository;
        this.søknadRepository = grunnlagProvider.getSøknadRepository();
    }

    @Override
    public Behandling opprettManuellRevurdering(Fagsak fagsak,
                                                BehandlingÅrsakType revurderingsÅrsak,
                                                OrganisasjonsEnhet enhet) {
        var behandling = opprettRevurdering(fagsak, revurderingsÅrsak, true, enhet);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst,
            List.of(AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING));
        return behandling;
    }

    @Override
    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak,
                                                   BehandlingÅrsakType revurderingsÅrsak,
                                                   OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, false, enhet);
    }

    private Behandling opprettRevurdering(Fagsak fagsak,
                                          BehandlingÅrsakType revurderingsÅrsak,
                                          boolean manueltOpprettet,
                                          OrganisasjonsEnhet enhet) {
        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()));

        // lås original behandling først
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling);

        // deretter opprett revurdering
        var revurdering = revurderingTjenesteFelles.opprettRevurderingsbehandling(revurderingsÅrsak, origBehandling,
            manueltOpprettet, enhet);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, revurdering);

        // Kopier vilkår (samme vilkår vurderes i Revurdering)
        revurderingTjenesteFelles.kopierVilkårsresultat(origBehandling, revurdering, kontekst);

        // Kopier grunnlagsdata
        this.kopierAlleGrunnlagFraTidligereBehandling(origBehandling, revurdering);

        return revurdering;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        var originalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        pleiepengerRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        uføretrygdRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        nesteSakRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        if (BehandlingType.REVURDERING.equals(ny.getType())) {
            ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        } else {
            ytelsesFordelingRepository.hentAggregatHvisEksisterer(originalBehandlingId).ifPresent(yfa -> {
                var yfBuilder = YtelseFordelingAggregat.oppdatere(yfa)
                    .medOppgittRettighet(yfa.getOppgittRettighet())
                    .medOppgittDekningsgrad(yfa.getOppgittDekningsgrad());
                if (yfa.getOppgittFordeling() != null) {
                    var kopi = revurderingTjenesteFelles.kopierOppgittFordelingFraForrigeBehandling(
                        yfa.getOppgittFordeling());
                    yfBuilder.medOppgittFordeling(kopi);
                }
                ytelsesFordelingRepository.lagre(nyBehandlingId, yfBuilder.build());
            });
        }
        vergeRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        opptjeningIUtlandDokStatusRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandlingTilUtsattSøknad(Behandling original, Behandling ny) {
        var originalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(originalBehandlingId, nyBehandlingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(originalBehandlingId, nyBehandlingId);
        uføretrygdRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // Nytt til post-annullering
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(originalBehandlingId);
        var yfBuilder = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOppgittFordeling(ytelseFordelingAggregat.getOppgittFordeling())
            .medOppgittDekningsgrad(ytelseFordelingAggregat.getOppgittDekningsgrad())
            .medOppgittRettighet(ytelseFordelingAggregat.getOppgittRettighet());
        ytelsesFordelingRepository.lagre(nyBehandlingId, yfBuilder.build());
        var originalSøknad = søknadRepository.hentSøknad(originalBehandlingId);
        var søknadBuilder = new SøknadEntitet.Builder(originalSøknad, true).medErEndringssøknad(false);
        søknadRepository.lagreOgFlush(ny, søknadBuilder.build());

        opptjeningIUtlandDokStatusRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        var sisteVedtakAnnullert = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()))
            .map(Behandlingsresultat::getBehandlingResultatType)
            .filter(BehandlingResultatType.FORELDREPENGER_SENERE::equals)
            .isPresent();
        return !sisteVedtakAnnullert && revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return revurderingEndring.erRevurderingMedUendretUtfall(behandling);
    }
}
