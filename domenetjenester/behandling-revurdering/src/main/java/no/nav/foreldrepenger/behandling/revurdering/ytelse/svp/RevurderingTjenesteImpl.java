package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValgRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class RevurderingTjenesteImpl implements RevurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private RevurderingEndring revurderingEndring;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VergeRepository vergeRepository;
    private NesteSakRepository nesteSakRepository;
    private ArbeidsforholdValgRepository arbeidsforholdValgRepository;

    public RevurderingTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteImpl(BehandlingRepository behandlingRepository,
                                   BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   InntektArbeidYtelseTjeneste iayTjeneste,
                                   @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) RevurderingEndring revurderingEndring,
                                   RevurderingTjenesteFelles revurderingTjenesteFelles,
                                   VergeRepository vergeRepository) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.familieHendelseRepository = grunnlagRepositoryProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = grunnlagRepositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = grunnlagRepositoryProvider.getMedlemskapRepository();
        this.svangerskapspengerRepository = grunnlagRepositoryProvider.getSvangerskapspengerRepository();
        this.nesteSakRepository = grunnlagRepositoryProvider.getNesteSakRepository();
        this.revurderingEndring = revurderingEndring;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.vergeRepository = vergeRepository;
        this.arbeidsforholdValgRepository = grunnlagRepositoryProvider.getArbeidsforholdValgRepository();
    }

    @Override
    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak,
                                                OrganisasjonsEnhet enhet, String opprettetAv) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, true, enhet, opprettetAv);
    }

    @Override
    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak,
                                                   BehandlingÅrsakType revurderingsÅrsak,
                                                   OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, false, enhet, null);
    }

    private Behandling opprettRevurdering(Fagsak fagsak,
                                          BehandlingÅrsakType revurderingsÅrsak,
                                          boolean manueltOpprettet,
                                          OrganisasjonsEnhet enhet, String opprettetAv) {
        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()));

        // lås original behandling først
        var originalLås = behandlingRepository.taSkriveLås(origBehandling.getId());
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling, originalLås);

        // deretter opprett revurdering
        var revurdering = revurderingTjenesteFelles.opprettRevurderingsbehandling(revurderingsÅrsak, origBehandling,
            manueltOpprettet, enhet, opprettetAv);
        var revurderingLås = behandlingRepository.taSkriveLås(revurdering.getId());
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering, revurderingLås);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, revurdering);
        revurderingTjenesteFelles.opprettHistorikkInnslagForNyRevurdering(revurdering, revurderingsÅrsak, manueltOpprettet);

        // Kopier vilkår (samme vilkår vurderes i Revurdering)
        revurderingTjenesteFelles.kopierVilkårsresultat(origBehandling, revurdering, kontekst, Set.of(VilkårType.SVANGERSKAPSPENGERVILKÅR));

        // Kopier grunnlagsdata
        this.kopierAlleGrunnlagFraTidligereBehandling(origBehandling, revurdering);

        return revurdering;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        var originalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        svangerskapspengerRepository.kopierSvpGrunnlagFraEksisterendeBehandling(originalBehandlingId, ny);
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        vergeRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        nesteSakRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        arbeidsforholdValgRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return revurderingEndring.erRevurderingMedUendretUtfall(behandling);
    }
}
