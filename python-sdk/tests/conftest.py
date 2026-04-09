import pytest

from toeic_client import ToeicClient


@pytest.fixture
def client(httpx_mock):
    with ToeicClient(base_url="http://test/api/v1", token="fake-token") as c:
        yield c
