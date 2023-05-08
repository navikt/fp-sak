package no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
class ArbeidsforholdValgRepositoryTest {
    private ArbeidsforholdValgRepository arbeidsforholdValgRepository;
    private EntityManager entityManager;

    @BeforeEach
    void setup(EntityManager entityManager) {
        this.entityManager = entityManager;
        arbeidsforholdValgRepository = new ArbeidsforholdValgRepository(entityManager);
    }

    private Behandling opprettBehandling() {
        return new BasicBehandlingBuilder(entityManager).opprettOgLagreFørstegangssøknad(
            FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    void lagre_en_vurdering_på_nytt_grunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var vurdering = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)
            .medArbeidsgiver("999999999")
            .medBegrunnelse("Dette er en begrunnelse")
            .build();

        // Act
        arbeidsforholdValgRepository.lagre(vurdering, behandling.getId());
        var notater = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandling.getId());

        // Assert
        assertThat(notater)
            .isNotNull()
            .hasSize(1);
        var vurderingEntitet = notater.get(0);
        assertThat(vurderingEntitet.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);
        assertThat(vurderingEntitet.getBegrunnelse()).isEqualTo("Dette er en begrunnelse");
        assertThat(vurderingEntitet.getArbeidsgiver().getIdentifikator()).isEqualTo("999999999");
    }

    @Test
    void lagre_to_like_vurderinger_uten_at_begge_er_aktive_og_like() {
        // Arrange
        var behandling = opprettBehandling();
        var vurdering = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)
            .medArbeidsgiver("999999999")
            .medBegrunnelse("Dette er en begrunnelse")
            .build();


        var kopi = ArbeidsforholdValg.kopier(vurdering).build();
        vurdering.setBehandlingId(behandling.getId());
        kopi.setBehandlingId(behandling.getId());

        // Act
        arbeidsforholdValgRepository.lagre(vurdering, behandling.getId());
        arbeidsforholdValgRepository.lagre(kopi, behandling.getId());
        var notater = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandling.getId());

        // Assert
        assertThat(notater)
            .isNotNull()
            .hasSize(1);
        var vurderingEntitet = notater.get(0);
        assertThat(vurderingEntitet.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);
        assertThat(vurderingEntitet.getBegrunnelse()).isEqualTo("Dette er en begrunnelse");
        assertThat(vurderingEntitet.getArbeidsgiver().getIdentifikator()).isEqualTo("999999999");
    }

    @Test
    void skal_kunne_slette_valg_gjort() {
        // Arrange
        var behandling = opprettBehandling();
        var vurdering = ArbeidsforholdValg.builder()
                .medVurdering(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)
                .medArbeidsgiver("999999999")
                .medBegrunnelse("Dette er en begrunnelse")
                .build();

        // Act
        arbeidsforholdValgRepository.lagre(vurdering, behandling.getId());
        var valg = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandling.getId());

        // Assert
        assertThat(valg)
            .isNotNull()
            .hasSize(1);
        var vurderingEntitet = valg.get(0);
        assertThat(vurderingEntitet.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);
        assertThat(vurderingEntitet.getBegrunnelse()).isEqualTo("Dette er en begrunnelse");
        assertThat(vurderingEntitet.getArbeidsgiver().getIdentifikator()).isEqualTo("999999999");

        // Act 2
        arbeidsforholdValgRepository.fjernValg(vurdering);
        var nyeValg = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandling.getId());

        assertThat(nyeValg).isEmpty();
    }


    @Test
    void lagre_flere_vurderinger_på_samme_grunnlag() {
        // Arrange
        var behandling = opprettBehandling();
        var vurdering1 = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)
            .medArbeidsgiver("999999999")
            .medBegrunnelse("Dette er en begrunnelse")
            .build();
        var vurdering2 = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING)
            .medArbeidsgiver("342352362")
            .medBegrunnelse("Dette er en annen begrunnelse")
            .build();

        // Act
        arbeidsforholdValgRepository.lagre(vurdering1, behandling.getId());
        arbeidsforholdValgRepository.lagre(vurdering2, behandling.getId());
        var notater = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandling.getId());

        // Assert
        assertThat(notater)
            .isNotNull()
            .hasSize(2);
        var entitet1 = finnVurderingFor(notater, "999999999");
        assertThat(entitet1).isNotNull();
        assertThat(entitet1.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);
        assertThat(entitet1.getBegrunnelse()).isEqualTo("Dette er en begrunnelse");

        var entitet2 = finnVurderingFor(notater, "342352362");
        assertThat(entitet2).isNotNull();
        assertThat(entitet2.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING);
        assertThat(entitet2.getBegrunnelse()).isEqualTo("Dette er en annen begrunnelse");
    }

    @Test
    void lagre_flere_vurderinger_hos_samme_ag() {
        // Arrange
        var behandling = opprettBehandling();
        var ref1 = InternArbeidsforholdRef.nyRef();
        var vurdering1 = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)
            .medArbeidsgiver("999999999")
            .medBegrunnelse("Dette er en begrunnelse")
            .medArbeidsforholdRef(ref1)
            .build();
        var ref2 = InternArbeidsforholdRef.nyRef();
        var vurdering2 = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING)
            .medArbeidsgiver("999999999")
            .medArbeidsforholdRef(ref2)
            .medBegrunnelse("Dette er en annen begrunnelse")
            .build();

        // Act
        arbeidsforholdValgRepository.lagre(vurdering1, behandling.getId());
        arbeidsforholdValgRepository.lagre(vurdering2, behandling.getId());
        var notater = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandling.getId());

        // Assert
        assertThat(notater)
            .isNotNull()
            .hasSize(2);
        var entitet1 = finnVurderingFor(notater, "999999999", ref1);
        assertThat(entitet1).isNotNull();
        assertThat(entitet1.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);
        assertThat(entitet1.getBegrunnelse()).isEqualTo("Dette er en begrunnelse");

        var entitet2 = finnVurderingFor(notater, "999999999", ref2);
        assertThat(entitet2).isNotNull();
        assertThat(entitet2.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING);
        assertThat(entitet2.getBegrunnelse()).isEqualTo("Dette er en annen begrunnelse");
    }

    @Test
    void lagre_ny_vurdering_på_eksisterende_arbeidsforhold_uten_å_berøre_annet_arbeidsforhold() {
        // Arrange 1
        var behandling = opprettBehandling();
        var vurdering1 = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)
            .medArbeidsgiver("999999999")
            .medBegrunnelse("Dette er en begrunnelse")
            .build();
        var vurdering2 = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING)
            .medArbeidsgiver("342352362")
            .medBegrunnelse("Dette er en annen begrunnelse")
            .build();

        // Act 1
        arbeidsforholdValgRepository.lagre(vurdering1, behandling.getId());
        arbeidsforholdValgRepository.lagre(vurdering2, behandling.getId());
        var vurderinger = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandling.getId());

        // Assert 1
        assertThat(vurderinger)
            .isNotNull()
            .hasSize(2);
        var entitet1 = finnVurderingFor(vurderinger, "999999999");
        assertThat(entitet1).isNotNull();
        assertThat(entitet1.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);
        assertThat(entitet1.getBegrunnelse()).isEqualTo("Dette er en begrunnelse");

        var entitet2 = finnVurderingFor(vurderinger, "342352362");
        assertThat(entitet2).isNotNull();
        assertThat(entitet2.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING);
        assertThat(entitet2.getBegrunnelse()).isEqualTo("Dette er en annen begrunnelse");

        // Arrange 2
        var nyVurdering1 = ArbeidsforholdValg.builder()
            .medVurdering(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING)
            .medArbeidsgiver("999999999")
            .medBegrunnelse("Nei jeg gjorde noe feil")
            .build();

        // Act 2
        arbeidsforholdValgRepository.lagre(nyVurdering1, behandling.getId());
        var nyeVurderinger = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandling.getId());

        // Assert 2
        assertThat(nyeVurderinger)
            .isNotNull()
            .hasSize(2);
        var nyEntitet1 = finnVurderingFor(nyeVurderinger, "999999999");
        assertThat(nyEntitet1).isNotNull();
        assertThat(nyEntitet1.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING);
        assertThat(nyEntitet1.getBegrunnelse()).isEqualTo("Nei jeg gjorde noe feil");

        var urørtEntitet = finnVurderingFor(nyeVurderinger, "342352362");
        assertThat(urørtEntitet).isNotNull();
        assertThat(urørtEntitet.getVurdering()).isEqualTo(ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING);
        assertThat(urørtEntitet.getBegrunnelse()).isEqualTo("Dette er en annen begrunnelse");

    }


    private ArbeidsforholdValg finnVurderingFor(List<ArbeidsforholdValg> vurderteArbeidsforhold, String orgnr, InternArbeidsforholdRef ref) {
        return vurderteArbeidsforhold.stream()
            .filter(vurd -> vurd.getArbeidsgiver().getIdentifikator().equals(orgnr))
            .filter(vurd -> vurd.getArbeidsforholdRef().gjelderFor(ref))
            .findFirst()
            .orElse(null);
    }

    private ArbeidsforholdValg finnVurderingFor(List<ArbeidsforholdValg> vurderteArbeidsforhold, String orgnr) {
        return finnVurderingFor(vurderteArbeidsforhold, orgnr, InternArbeidsforholdRef.nullRef());
    }


}
