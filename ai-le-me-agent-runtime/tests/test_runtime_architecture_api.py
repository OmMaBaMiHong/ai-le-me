from fastapi.testclient import TestClient

from agent_runtime.app import create_app


def test_runtime_architecture_exposes_product_spine_and_planes():
    client = TestClient(create_app())

    response = client.get("/runtime/architecture")

    assert response.status_code == 200
    body = response.json()
    assert body["north_star"]
    assert body["docs_path"].endswith("agent-product-architecture.md")

    runtime_spine = {item["code"]: item for item in body["runtime_spine"]}
    assert "policy" in runtime_spine
    assert "eval" in runtime_spine

    planes = {item["code"]: item for item in body["product_planes"]}
    assert "governance_economics" in planes
    assert "operations_improvement" in planes

    products = {item["code"]: item for item in body["core_products"]}
    assert "companion_agent" in products
    assert "policy" in products["companion_agent"]["required_layers"]

    policy_surfaces = {item["code"]: item for item in body["policy_surfaces"]}
    assert "economics" in policy_surfaces
    assert any(check["code"] == "provider_budget" for check in policy_surfaces["economics"]["checks"])

    eval_metrics = {item["code"]: item for item in body["eval_metrics"]}
    assert "cost_per_success" in eval_metrics
